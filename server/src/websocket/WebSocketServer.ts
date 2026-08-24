import { WebSocketServer, WebSocket } from 'ws';
import { Server as HttpServer } from 'http';
import { AuthManager } from '../auth/AuthManager';
import { Store } from '../database/Store';
import { SyncEngine } from '../collaboration/SyncEngine';
import { WsMessage, UserProfile } from '../types';

interface AuthenticatedClient {
  ws: WebSocket;
  user: UserProfile;
  currentPlanId?: string;
  isAlive: boolean;
}

export class CollaborationWebSocketServer {
  private wss: WebSocketServer;
  private authManager: AuthManager;
  private store: Store;
  private syncEngine: SyncEngine;

  // Track clients and room memberships
  private clients: Map<WebSocket, AuthenticatedClient> = new Map();
  private planRooms: Map<string, Set<WebSocket>> = new Map();

  constructor(server: HttpServer) {
    this.wss = new WebSocketServer({ server, path: '/ws' });
    this.authManager = new AuthManager();
    this.store = Store.getInstance();
    this.syncEngine = new SyncEngine();

    this.setupServer();
    this.setupHeartbeat();
  }

  private setupServer(): void {
    this.wss.on('connection', (ws: WebSocket) => {
      const clientInfo: AuthenticatedClient = {
        ws,
        user: { userId: '', displayName: 'Guest', createdAt: Date.now() },
        isAlive: true
      };
      this.clients.set(ws, clientInfo);

      ws.on('pong', () => {
        const client = this.clients.get(ws);
        if (client) client.isAlive = true;
      });

      ws.on('message', (data: string) => {
        try {
          const message: WsMessage = JSON.parse(data.toString());
          this.handleMessage(ws, message);
        } catch (e: any) {
          this.send(ws, {
            type: 'ERROR',
            error: `Malformed JSON message: ${e?.message || 'Unknown error'}`
          });
        }
      });

      ws.on('close', () => {
        this.handleDisconnect(ws);
      });

      ws.on('error', (err) => {
        console.warn('WebSocket client error:', err);
        this.handleDisconnect(ws);
      });
    });
  }

  private setupHeartbeat(): void {
    setInterval(() => {
      this.clients.forEach((client, ws) => {
        if (!client.isAlive) {
          this.handleDisconnect(ws);
          ws.terminate();
          return;
        }
        client.isAlive = false;
        try {
          ws.ping();
        } catch (e) {
          // Ignore
        }
      });
    }, 30000);
  }

  private handleMessage(ws: WebSocket, message: WsMessage): void {
    const client = this.clients.get(ws);
    if (!client) return;

    switch (message.type) {
      case 'AUTH': {
        const user = this.authManager.validateToken(message.token);
        if (!user) {
          this.send(ws, {
            type: 'ERROR',
            error: 'Authentication failed. Invalid or expired token.'
          });
          return;
        }

        client.user = user;
        this.send(ws, {
          type: 'AUTH_ACK',
          success: true,
          userId: user.userId,
          userName: user.displayName
        });
        break;
      }

      case 'JOIN_ROOM': {
        // Authenticate via token in message if not yet authenticated
        if (!client.user.userId && message.token) {
          const user = this.authManager.validateToken(message.token);
          if (user) {
            client.user = user;
          }
        }

        if (!client.user.userId) {
          this.send(ws, {
            type: 'ERROR',
            error: 'Cannot join room: Not authenticated.'
          });
          return;
        }

        const planId = message.planId;
        if (!planId) {
          this.send(ws, { type: 'ERROR', error: 'Plan ID required to join room.' });
          return;
        }

        // Verify membership access control
        const plan = this.store.getPlanById(planId);
        if (!plan) {
          this.send(ws, { type: 'ERROR', error: 'Plan not found.' });
          return;
        }

        if (plan.ownerId !== client.user.userId && plan.partnerId !== client.user.userId) {
          this.send(ws, {
            type: 'ERROR',
            error: 'Access denied: You are not a member of this plan.'
          });
          return;
        }

        // Leave previous room if any
        if (client.currentPlanId && client.currentPlanId !== planId) {
          this.leaveCurrentRoom(ws, client);
        }

        // Add to new room
        client.currentPlanId = planId;
        let room = this.planRooms.get(planId);
        if (!room) {
          room = new Set();
          this.planRooms.set(planId, room);
        }
        room.add(ws);

        // Check if partner is also in room
        const partnerOnline = Array.from(room).some(otherWs => {
          const otherClient = this.clients.get(otherWs);
          return otherClient && otherClient.user.userId !== client.user.userId;
        });

        // Send JOIN_ACK with authoritative document state
        this.send(ws, {
          type: 'JOIN_ACK',
          success: true,
          planId: plan.planId,
          document: {
            planId: plan.planId,
            title: plan.title,
            description: plan.description,
            revision: plan.revision,
            blocks: plan.blocks,
            ownerId: plan.ownerId,
            ownerName: plan.ownerName,
            partnerId: plan.partnerId,
            partnerName: plan.partnerName,
            inviteCode: plan.inviteCode
          },
          presence: {
            userId: client.user.userId,
            userName: client.user.displayName,
            status: partnerOnline ? 'ONLINE' : 'OFFLINE',
            isPartner: true
          }
        });

        // Broadcast to other participants in this plan that user joined
        this.broadcastToRoom(planId, ws, {
          type: 'PRESENCE',
          presence: {
            userId: client.user.userId,
            userName: client.user.displayName,
            status: 'ONLINE',
            isPartner: true
          }
        });
        break;
      }

      case 'OPERATION': {
        if (!client.user.userId) {
          this.send(ws, { type: 'ERROR', error: 'Unauthorized: Authenticate first.' });
          return;
        }

        if (!message.operation) {
          this.send(ws, { type: 'ERROR', error: 'Missing operation in message.' });
          return;
        }

        const result = this.syncEngine.processOperation(client.user.userId, message.operation);
        if (!result.success || !result.operation) {
          this.send(ws, {
            type: 'ERROR',
            error: result.error || 'Failed to process operation.'
          });
          return;
        }

        // Send ACK back to sender
        this.send(ws, {
          type: 'OPERATION_ACK',
          success: true,
          operation: result.operation,
          changeRecord: result.changeRecord
        });

        // Broadcast remote operation to partner with change metadata
        this.broadcastToRoom(result.operation.planId, ws, {
          type: 'REMOTE_OPERATION',
          operation: result.operation,
          changeRecord: result.changeRecord
        });
        break;
      }

      case 'TYPING': {
        if (!client.user.userId || !client.currentPlanId) return;

        this.broadcastToRoom(client.currentPlanId, ws, {
          type: 'TYPING',
          typing: {
            userId: client.user.userId,
            userName: client.user.displayName,
            isTyping: !!message.typing?.isTyping
          }
        });
        break;
      }

      case 'LEAVE_ROOM': {
        this.leaveCurrentRoom(ws, client);
        break;
      }
    }
  }

  private leaveCurrentRoom(ws: WebSocket, client: AuthenticatedClient): void {
    if (!client.currentPlanId) return;

    const planId = client.currentPlanId;
    const room = this.planRooms.get(planId);
    if (room) {
      room.delete(ws);
      if (room.size === 0) {
        this.planRooms.delete(planId);
      }
    }

    // Broadcast user left/offline to remaining participants
    this.broadcastToRoom(planId, ws, {
      type: 'PRESENCE',
      presence: {
        userId: client.user.userId,
        userName: client.user.displayName,
        status: 'OFFLINE',
        isPartner: true
      }
    });

    client.currentPlanId = undefined;
  }

  private handleDisconnect(ws: WebSocket): void {
    const client = this.clients.get(ws);
    if (client) {
      this.leaveCurrentRoom(ws, client);
      this.clients.delete(ws);
    }
  }

  private broadcastToRoom(planId: string, senderWs: WebSocket, message: WsMessage): void {
    const room = this.planRooms.get(planId);
    if (!room) return;

    const data = JSON.stringify(message);
    room.forEach((clientWs) => {
      if (clientWs !== senderWs && clientWs.readyState === WebSocket.OPEN) {
        try {
          clientWs.send(data);
        } catch (e) {
          console.warn('Failed to send broadcast to client', e);
        }
      }
    });
  }

  private send(ws: WebSocket, message: WsMessage): void {
    if (ws.readyState === WebSocket.OPEN) {
      try {
        ws.send(JSON.stringify(message));
      } catch (e) {
        console.warn('Failed to send message to client', e);
      }
    }
  }
}
