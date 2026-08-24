import http from 'http';
import express, { Request, Response } from 'express';
import cors from 'cors';
import { AuthManager } from './auth/AuthManager';
import { PlanManager } from './plans/PlanManager';
import { CollaborationWebSocketServer } from './websocket/WebSocketServer';

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const wsServer = new CollaborationWebSocketServer(server);
const authManager = new AuthManager();
const planManager = new PlanManager();

// --- HTTP Endpoints ---

// Health Check
app.get('/api/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', time: Date.now(), service: 'DuoPlan Collaboration Server' });
});

// Authentication / Session
app.post('/api/auth/session', (req: Request, res: Response) => {
  const { userId, displayName } = req.body;
  const result = authManager.authenticateOrCreate(userId, displayName);
  res.json({
    success: true,
    user: result.user,
    token: result.session.token,
    expiresAt: result.session.expiresAt
  });
});

// Create Shared Plan
app.post('/api/plans/create', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || req.body.token;
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const { title, description, blocks } = req.body;
  const plan = planManager.createSharedPlan(user, title || 'Shared Plan', description || '', blocks);

  res.json({
    success: true,
    plan
  });
});

// Join Shared Plan with Invite Code
app.post('/api/plans/join', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || req.body.token;
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const { inviteCode } = req.body;
  if (!inviteCode || typeof inviteCode !== 'string') {
    res.status(400).json({ success: false, error: 'Please provide a valid invite code.' });
    return;
  }

  const result = planManager.joinSharedPlan(user, inviteCode);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.json({
    success: true,
    plan: result.plan
  });
});

// Get Plan Details
app.get('/api/plans/:planId', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || (req.query.token as string);
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const planId = Array.isArray(req.params.planId) ? req.params.planId[0] : req.params.planId;
  const result = planManager.getSharedPlan(user.userId, planId);
  if (!result.success) {
    res.status(403).json(result);
    return;
  }

  res.json({
    success: true,
    plan: result.plan
  });
});

// Get Plan History
app.get('/api/plans/:planId/history', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || (req.query.token as string);
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const planId = Array.isArray(req.params.planId) ? req.params.planId[0] : req.params.planId;
  const result = planManager.getPlanHistory(user.userId, planId);
  if (!result.success) {
    res.status(403).json(result);
    return;
  }

  res.json({
    success: true,
    history: result.history || []
  });
});

// Acknowledge Plan History
app.post('/api/plans/:planId/history/acknowledge', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || req.body.token;
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const planId = Array.isArray(req.params.planId) ? req.params.planId[0] : req.params.planId;
  const result = planManager.acknowledgeHistory(user.userId, planId);
  res.json(result);
});

// Leave Shared Plan
app.post('/api/plans/:planId/leave', (req: Request, res: Response) => {
  const token = req.headers.authorization?.replace('Bearer ', '') || req.body.token;
  const user = authManager.validateToken(token);

  if (!user) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid authentication token.' });
    return;
  }

  const planId = Array.isArray(req.params.planId) ? req.params.planId[0] : req.params.planId;
  const result = planManager.leavePlan(user.userId, planId);
  res.json(result);
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
  console.log(`DuoPlan Collaboration Server running on port ${PORT}`);
  console.log(`WebSocket endpoint ready at ws://localhost:${PORT}/ws`);
});
