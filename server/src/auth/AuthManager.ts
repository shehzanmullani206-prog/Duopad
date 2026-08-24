import { Store } from '../database/Store';
import { UserProfile, UserSession } from '../types';

export class AuthManager {
  private store: Store;

  constructor(store: Store = Store.getInstance()) {
    this.store = store;
  }

  public authenticateOrCreate(userId?: string, displayName: string = 'Collaborator'): { user: UserProfile; session: UserSession } {
    return this.store.getOrCreateUser(userId, displayName);
  }

  public validateToken(token?: string): UserProfile | null {
    if (!token || token.trim().length === 0) return null;
    return this.store.getUserByToken(token.trim());
  }
}
