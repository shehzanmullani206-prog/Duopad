import { Store } from '../database/Store';
import { UserProfile, SharedPlan, EditorBlock } from '../types';

export class PlanManager {
  private store: Store;

  constructor(store: Store = Store.getInstance()) {
    this.store = store;
  }

  public createSharedPlan(
    user: UserProfile,
    title: string,
    description: string,
    initialBlocks?: EditorBlock[]
  ): SharedPlan {
    return this.store.createPlan(user, title, description, initialBlocks);
  }

  public joinSharedPlan(user: UserProfile, inviteCode: string): { success: boolean; error?: string; plan?: SharedPlan } {
    return this.store.joinPlan(user, inviteCode);
  }

  public getSharedPlan(userId: string, planId: string): { success: boolean; error?: string; plan?: SharedPlan } {
    const plan = this.store.getPlanById(planId);
    if (!plan) {
      return { success: false, error: 'Plan not found.' };
    }

    if (plan.ownerId !== userId && plan.partnerId !== userId) {
      return { success: false, error: 'Unauthorized: You are not a participant of this plan.' };
    }

    return { success: true, plan };
  }

  public removePartner(ownerUserId: string, planId: string): { success: boolean; error?: string } {
    return this.store.removePartner(ownerUserId, planId);
  }

  public leavePlan(userId: string, planId: string): { success: boolean; error?: string } {
    return this.store.leavePlan(userId, planId);
  }

  public getPlanHistory(userId: string, planId: string): { success: boolean; error?: string; history?: any[] } {
    const plan = this.store.getPlanById(planId);
    if (!plan) {
      return { success: false, error: 'Plan not found.' };
    }

    if (plan.ownerId !== userId && plan.partnerId !== userId) {
      return { success: false, error: 'Unauthorized: You are not a participant of this plan.' };
    }

    const history = this.store.getPlanHistory(planId);
    return { success: true, history };
  }

  public acknowledgeHistory(userId: string, planId: string): { success: boolean; error?: string } {
    const plan = this.store.getPlanById(planId);
    if (!plan) {
      return { success: false, error: 'Plan not found.' };
    }

    if (plan.ownerId !== userId && plan.partnerId !== userId) {
      return { success: false, error: 'Unauthorized: You are not a participant of this plan.' };
    }

    this.store.markPlanHistoryAcknowledged(planId, userId);
    return { success: true };
  }
}
