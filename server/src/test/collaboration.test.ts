import { Store } from '../database/Store';
import { AuthManager } from '../auth/AuthManager';
import { PlanManager } from '../plans/PlanManager';
import { SyncEngine } from '../collaboration/SyncEngine';
import { CollaborationOperation, EditorBlock } from '../types';

function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(`Assertion Failed: ${message}`);
  }
}

async function runTests() {
  console.log('--- Starting Collaboration Backend Unit Tests ---');
  const store = Store.getInstance();
  const auth = new AuthManager(store);
  const plans = new PlanManager(store);
  const sync = new SyncEngine(store);

  // 1. Auth & Profiles
  const userA = auth.authenticateOrCreate('user-a-1', 'Alice');
  const userB = auth.authenticateOrCreate('user-b-2', 'Bob');
  const userC = auth.authenticateOrCreate('user-c-3', 'Charlie');

  assert(userA.user.displayName === 'Alice', 'User A name should be Alice');
  assert(userB.user.displayName === 'Bob', 'User B name should be Bob');
  assert(auth.validateToken(userA.session.token) !== null, 'User A token must be valid');

  console.log('✓ Auth & user sessions verified');

  // 2. Create Shared Plan
  const plan = plans.createSharedPlan(userA.user, 'Weekend Roadtrip', 'Planning trip with Bob');
  assert(plan.ownerId === 'user-a-1', 'Owner must be User A');
  assert(plan.partnerId === null, 'Partner should be initially null');
  assert(plan.inviteCode.length >= 6, 'Invite code must be generated');
  assert(plan.revision === 1, 'Initial revision should be 1');

  console.log(`✓ Shared plan created with invite code: ${plan.inviteCode}`);

  // 3. User B joins using invite code
  const joinResult = plans.joinSharedPlan(userB.user, plan.inviteCode);
  assert(joinResult.success === true, 'User B join should succeed');
  assert(joinResult.plan?.partnerId === 'user-b-2', 'Partner ID should be User B');
  assert(joinResult.plan?.partnerName === 'Bob', 'Partner Name should be Bob');

  console.log('✓ User B successfully joined plan as partner');

  // 4. Two-Person Limit: Third user (User C) attempts to join
  const joinResultC = plans.joinSharedPlan(userC.user, plan.inviteCode);
  assert(joinResultC.success === false, 'User C must be rejected from joining a 2-person plan');
  assert(joinResultC.error?.includes('two participants') === true, 'Error message should indicate two participants limit');

  console.log('✓ Server-side 2-person limit successfully enforced');

  // 5. Realtime Operations & Revision Increment
  const firstBlockId = plan.blocks[0].id;
  const op1: CollaborationOperation = {
    operationId: 'op-101',
    planId: plan.planId,
    userId: 'user-a-1',
    type: 'BLOCK_UPDATE',
    revision: 1,
    payload: JSON.stringify({
      block: {
        id: firstBlockId,
        type: 'HEADING',
        text: 'Depart at 8:00 AM on Saturday',
        spans: [{ start: 0, end: 6, isBold: true, textColorHex: '#60A5FA' }],
        headingLevel: 'H2',
        alignment: 'LEFT',
        isChecked: false,
        numberIndex: 1,
        lineSpacing: 1.4,
        letterSpacing: 0.3,
        paragraphSpacingDp: 8
      }
    }),
    timestamp: Date.now()
  };

  const opResult1 = sync.processOperation('user-a-1', op1);
  assert(opResult1.success === true, 'Operation 1 must succeed');
  assert(opResult1.updatedPlan?.revision === 2, 'Revision should increment to 2');
  assert(opResult1.updatedPlan?.blocks[0].text === 'Depart at 8:00 AM on Saturday', 'Block text must match');
  assert(opResult1.updatedPlan?.blocks[0].spans[0].isBold === true, 'Rich formatting span must be preserved');

  console.log('✓ Operation 1 processed, rich formatting preserved, revision -> 2');

  // 6. Reverse Operation by User B
  const op2: CollaborationOperation = {
    operationId: 'op-102',
    planId: plan.planId,
    userId: 'user-b-2',
    type: 'BLOCK_INSERT',
    revision: 2,
    payload: JSON.stringify({
      index: 1,
      block: {
        id: 'block-2',
        type: 'CHECKLIST',
        text: 'Pack hiking gear & snacks',
        spans: [],
        headingLevel: 'NORMAL',
        alignment: 'LEFT',
        isChecked: true,
        numberIndex: 1,
        lineSpacing: 1.4,
        letterSpacing: 0.3,
        paragraphSpacingDp: 8
      }
    }),
    timestamp: Date.now()
  };

  const opResult2 = sync.processOperation('user-b-2', op2);
  assert(opResult2.success === true, 'Operation 2 must succeed');
  assert(opResult2.updatedPlan?.revision === 3, 'Revision should increment to 3');
  assert(opResult2.updatedPlan?.blocks.length === 2, 'Plan should now contain 2 blocks');
  assert(opResult2.updatedPlan?.blocks[1].isChecked === true, 'Checklist status preserved');

  console.log('✓ Reverse editing by User B processed, revision -> 3');

  // 7. Duplicate Operation Protection
  const duplicateResult = sync.processOperation('user-a-1', op1);
  assert(duplicateResult.success === true, 'Duplicate operation handled gracefully');
  assert(duplicateResult.updatedPlan?.revision === 3, 'Revision must not increment for duplicate op');

  console.log('✓ Duplicate operation correctly ignored');

  // 8. Access Control: Unauthorized user attempts to edit
  const unauthorizedOp: CollaborationOperation = {
    operationId: 'op-999',
    planId: plan.planId,
    userId: 'user-c-3',
    type: 'BLOCK_UPDATE',
    revision: 3,
    payload: '{}',
    timestamp: Date.now()
  };
  const unauthResult = sync.processOperation('user-c-3', unauthorizedOp);
  assert(unauthResult.success === false, 'Unauthorized user edit must be rejected');

  console.log('✓ Unauthorized edit rejected');

  console.log('\n ALL BACKEND COLLABORATION TESTS PASSED SUCCESSFULLY! \n');
}

runTests().catch((err) => {
  console.error('Test Failed:', err);
  process.exit(1);
});
