import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import { UserProfile, UserSession, SharedPlan, CollaborationOperation, EditorBlock, ChangeRecord } from '../types';

export class Store {
  private static instance: Store;
  private users: Map<string, UserProfile> = new Map();
  private sessions: Map<string, UserSession> = new Map();
  private plans: Map<string, SharedPlan> = new Map();
  private inviteCodeToPlanId: Map<string, string> = new Map();
  private processedOperationIds: Set<string> = new Set();
  private recentOpsByPlan: Map<string, CollaborationOperation[]> = new Map();
  private historyByPlan: Map<string, ChangeRecord[]> = new Map();

  private storageFile: string = path.join(__dirname, '../../data_store.json');

  private constructor() {
    this.loadFromDisk();
  }

  public static getInstance(): Store {
    if (!Store.instance) {
      Store.instance = new Store();
    }
    return Store.instance;
  }

  private loadFromDisk(): void {
    try {
      if (fs.existsSync(this.storageFile)) {
        const raw = fs.readFileSync(this.storageFile, 'utf-8');
        const data = JSON.parse(raw);
        if (data.users) {
          for (const u of data.users) {
            this.users.set(u.userId, u);
          }
        }
        if (data.plans) {
          for (const p of data.plans) {
            this.plans.set(p.planId, p);
            if (p.inviteCode) {
              this.inviteCodeToPlanId.set(p.inviteCode.toUpperCase(), p.planId);
            }
          }
        }
        if (data.history) {
          for (const [planId, records] of Object.entries(data.history)) {
            if (Array.isArray(records)) {
              this.historyByPlan.set(planId, records as ChangeRecord[]);
            }
          }
        }
      }
    } catch (e) {
      console.warn('Could not load persistent store, starting fresh', e);
    }
  }

  public saveToDisk(): void {
    try {
      const historyObj: Record<string, ChangeRecord[]> = {};
      for (const [planId, records] of this.historyByPlan.entries()) {
        historyObj[planId] = records;
      }
      const data = {
        users: Array.from(this.users.values()),
        plans: Array.from(this.plans.values()),
        history: historyObj
      };
      fs.writeFileSync(this.storageFile, JSON.stringify(data, null, 2), 'utf-8');
    } catch (e) {
      console.error('Failed to persist store to disk', e);
    }
  }

  // --- Users & Sessions ---

  public getOrCreateUser(userId: string | undefined, displayName: string): { user: UserProfile; session: UserSession } {
    const finalUserId = (userId && userId.trim().length > 0) ? userId.trim() : crypto.randomUUID();
    let user = this.users.get(finalUserId);

    if (!user) {
      user = {
        userId: finalUserId,
        displayName: displayName.trim() || 'Collaborator',
        avatarPlaceholder: displayName.trim().substring(0, 2).toUpperCase() || 'DU',
        createdAt: Date.now()
      };
      this.users.set(finalUserId, user);
    } else if (displayName.trim() && user.displayName !== displayName.trim()) {
      user.displayName = displayName.trim();
      user.avatarPlaceholder = displayName.trim().substring(0, 2).toUpperCase();
      this.users.set(finalUserId, user);
    }

    const token = crypto.randomBytes(24).toString('hex');
    const session: UserSession = {
      token,
      user,
      expiresAt: Date.now() + 1000 * 60 * 60 * 24 * 30 // 30 days
    };
    this.sessions.set(token, session);
    this.saveToDisk();

    return { user, session };
  }

  public getUserByToken(token: string): UserProfile | null {
    const session = this.sessions.get(token);
    if (!session) return null;
    if (session.expiresAt < Date.now()) {
      this.sessions.delete(token);
      return null;
    }
    return session.user;
  }

  public getUserById(userId: string): UserProfile | null {
    return this.users.get(userId) || null;
  }

  // --- Plans & Sharing ---

  public generateUniqueInviteCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Avoid confusing O, 0, I, 1
    for (let attempts = 0; attempts < 100; attempts++) {
      let part1 = '';
      let part2 = '';
      let part3 = '';
      for (let i = 0; i < 3; i++) part1 += chars.charAt(Math.floor(Math.random() * chars.length));
      for (let i = 0; i < 3; i++) part2 += chars.charAt(Math.floor(Math.random() * chars.length));
      for (let i = 0; i < 2; i++) part3 += chars.charAt(Math.floor(Math.random() * chars.length));
      const code = `${part1}-${part2}-${part3}`;
      if (!this.inviteCodeToPlanId.has(code)) {
        return code;
      }
    }
    return `DUO-${Date.now().toString(36).toUpperCase()}`;
  }

  public createPlan(
    owner: UserProfile,
    title: string,
    description: string,
    initialBlocks?: EditorBlock[]
  ): SharedPlan {
    const planId = crypto.randomUUID();
    const inviteCode = this.generateUniqueInviteCode();
    const now = Date.now();

    const defaultBlocks: EditorBlock[] = initialBlocks && initialBlocks.length > 0 ? initialBlocks : [
      {
        id: crypto.randomUUID(),
        type: 'PARAGRAPH',
        text: '',
        spans: [],
        headingLevel: 'NORMAL',
        alignment: 'LEFT',
        isChecked: false,
        numberIndex: 1,
        lineSpacing: 1.4,
        letterSpacing: 0.3,
        paragraphSpacingDp: 8
      }
    ];

    const plan: SharedPlan = {
      planId,
      title: title.trim() || 'Shared Plan',
      description: description.trim(),
      inviteCode,
      ownerId: owner.userId,
      ownerName: owner.displayName,
      partnerId: null,
      partnerName: null,
      revision: 1,
      blocks: defaultBlocks,
      createdAt: now,
      updatedAt: now
    };

    this.plans.set(planId, plan);
    this.inviteCodeToPlanId.set(inviteCode.toUpperCase(), planId);
    this.saveToDisk();

    return plan;
  }

  public getPlanById(planId: string): SharedPlan | null {
    return this.plans.get(planId) || null;
  }

  public getPlanByInviteCode(code: string): SharedPlan | null {
    const sanitized = code.trim().toUpperCase();
    const planId = this.inviteCodeToPlanId.get(sanitized);
    if (!planId) return null;
    return this.plans.get(planId) || null;
  }

  public joinPlan(user: UserProfile, inviteCode: string): { success: boolean; error?: string; plan?: SharedPlan } {
    const sanitized = inviteCode.trim().toUpperCase();
    const planId = this.inviteCodeToPlanId.get(sanitized);

    if (!planId) {
      return { success: false, error: 'Invalid invitation code. Plan not found.' };
    }

    const plan = this.plans.get(planId);
    if (!plan) {
      return { success: false, error: 'Plan not found.' };
    }

    // If user is already the owner
    if (plan.ownerId === user.userId) {
      return { success: true, plan };
    }

    // If user is already the partner
    if (plan.partnerId === user.userId) {
      return { success: true, plan };
    }

    // Strict two-person limit: check if plan already has a partner
    if (plan.partnerId !== null && plan.partnerId !== user.userId) {
      return { success: false, error: 'This plan already has two participants.' };
    }

    // Assign as partner
    plan.partnerId = user.userId;
    plan.partnerName = user.displayName;
    plan.updatedAt = Date.now();

    this.plans.set(planId, plan);
    this.saveToDisk();

    return { success: true, plan };
  }

  public removePartner(ownerUserId: string, planId: string): { success: boolean; error?: string } {
    const plan = this.plans.get(planId);
    if (!plan) return { success: false, error: 'Plan not found.' };
    if (plan.ownerId !== ownerUserId) return { success: false, error: 'Only the plan owner can remove a partner.' };

    plan.partnerId = null;
    plan.partnerName = null;
    plan.updatedAt = Date.now();
    this.saveToDisk();
    return { success: true };
  }

  public leavePlan(userId: string, planId: string): { success: boolean; error?: string } {
    const plan = this.plans.get(planId);
    if (!plan) return { success: false, error: 'Plan not found.' };

    if (plan.partnerId === userId) {
      plan.partnerId = null;
      plan.partnerName = null;
      plan.updatedAt = Date.now();
      this.saveToDisk();
      return { success: true };
    }

    if (plan.ownerId === userId) {
      // If owner leaves, if partner exists promote partner or delete
      if (plan.partnerId) {
        plan.ownerId = plan.partnerId;
        plan.ownerName = plan.partnerName || 'Partner';
        plan.partnerId = null;
        plan.partnerName = null;
      } else {
        this.plans.delete(planId);
        this.inviteCodeToPlanId.delete(plan.inviteCode);
      }
      this.saveToDisk();
      return { success: true };
    }

    return { success: false, error: 'User is not a participant in this plan.' };
  }

  public isUserParticipant(userId: string, planId: string): boolean {
    const plan = this.plans.get(planId);
    if (!plan) return false;
    return plan.ownerId === userId || plan.partnerId === userId;
  }

  // --- Duplicate Operation Cache ---

  public isOperationDuplicate(operationId: string): boolean {
    return this.processedOperationIds.has(operationId);
  }

  public markOperationProcessed(operationId: string): void {
    if (this.processedOperationIds.size > 2000) {
      const firstEntries = Array.from(this.processedOperationIds).slice(0, 500);
      for (const id of firstEntries) {
        this.processedOperationIds.delete(id);
      }
    }
    this.processedOperationIds.add(operationId);
  }

  public recordOperation(op: CollaborationOperation): void {
    let list = this.recentOpsByPlan.get(op.planId);
    if (!list) {
      list = [];
      this.recentOpsByPlan.set(op.planId, list);
    }
    list.push(op);
    if (list.length > 200) {
      list.shift();
    }
  }

  // --- Change History Tracking ---

  public addPlanChange(change: ChangeRecord): void {
    let list = this.historyByPlan.get(change.planId);
    if (!list) {
      list = [];
      this.historyByPlan.set(change.planId, list);
    }
    // Prevent duplicate changes with same changeId or operationId
    if (!list.some(c => c.changeId === change.changeId || c.operationId === change.operationId)) {
      list.push(change);
      // Keep up to 1000 history records per plan
      if (list.length > 1000) {
        list.shift();
      }
      this.saveToDisk();
    }
  }

  public getPlanHistory(planId: string): ChangeRecord[] {
    const list = this.historyByPlan.get(planId);
    return list ? [...list] : [];
  }

  public markPlanHistoryAcknowledged(planId: string, userId: string): void {
    const list = this.historyByPlan.get(planId);
    if (list) {
      for (const rec of list) {
        if (rec.userId !== userId) {
          rec.isAcknowledged = true;
        }
      }
      this.saveToDisk();
    }
  }

  public updatePlanDocument(
    planId: string,
    updater: (plan: SharedPlan) => void
  ): SharedPlan | null {
    const plan = this.plans.get(planId);
    if (!plan) return null;
    updater(plan);
    plan.updatedAt = Date.now();
    this.saveToDisk();
    return plan;
  }
}
