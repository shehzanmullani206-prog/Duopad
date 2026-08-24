import * as crypto from 'crypto';
import { Store } from '../database/Store';
import { CollaborationOperation, SharedPlan, EditorBlock, ChangeRecord, ChangeAction } from '../types';

export class SyncEngine {
  private store: Store;

  constructor(store: Store = Store.getInstance()) {
    this.store = store;
  }

  public processOperation(
    userId: string,
    operation: CollaborationOperation
  ): {
    success: boolean;
    error?: string;
    updatedPlan?: SharedPlan;
    operation?: CollaborationOperation;
    changeRecord?: ChangeRecord;
  } {
    // 1. Check operation validity
    if (!operation || !operation.operationId || !operation.planId || !operation.type) {
      return { success: false, error: 'Invalid operation structure.' };
    }

    // 2. Check duplicate operation protection
    if (this.store.isOperationDuplicate(operation.operationId)) {
      const plan = this.store.getPlanById(operation.planId);
      return { success: true, updatedPlan: plan || undefined, operation };
    }

    // 3. Check plan existence & membership access control
    const plan = this.store.getPlanById(operation.planId);
    if (!plan) {
      return { success: false, error: 'Plan not found.' };
    }

    if (plan.ownerId !== userId && plan.partnerId !== userId) {
      return { success: false, error: 'Unauthorized: Not a member of this plan.' };
    }

    // 4. Apply operation to authoritative server document state and generate ChangeRecord
    try {
      const changeRecord = this.applyOperationToPlan(plan, operation, userId);
      plan.revision += 1;
      operation.revision = plan.revision;

      if (changeRecord) {
        changeRecord.revision = plan.revision;
        this.store.addPlanChange(changeRecord);
      }

      // 5. Mark processed and record
      this.store.markOperationProcessed(operation.operationId);
      this.store.recordOperation(operation);
      this.store.saveToDisk();

      return {
        success: true,
        updatedPlan: plan,
        operation,
        changeRecord: changeRecord || undefined
      };
    } catch (e: any) {
      return { success: false, error: `Failed to apply operation: ${e?.message || 'Unknown error'}` };
    }
  }

  private applyOperationToPlan(
    plan: SharedPlan,
    operation: CollaborationOperation,
    userId: string
  ): ChangeRecord | null {
    let payloadData: any = null;
    try {
      payloadData = typeof operation.payload === 'string' && operation.payload.startsWith('{')
        ? JSON.parse(operation.payload)
        : operation.payload;
    } catch (e) {
      payloadData = operation.payload;
    }

    const userName = operation.userName || (userId === plan.ownerId ? plan.ownerName : plan.partnerName || 'Partner');
    const timestamp = operation.timestamp || Date.now();
    const changeId = crypto.randomUUID();

    switch (operation.type) {
      case 'BLOCK_UPDATE': {
        let blockId = '';
        let oldText = '';
        let newText = '';
        let blockType = 'PARAGRAPH';

        if (payloadData && payloadData.block) {
          const updatedBlock = payloadData.block as EditorBlock;
          blockId = updatedBlock.id;
          newText = updatedBlock.text;
          blockType = updatedBlock.type;
          const idx = plan.blocks.findIndex(b => b.id === updatedBlock.id);
          if (idx !== -1) {
            oldText = plan.blocks[idx].text;
            plan.blocks[idx] = updatedBlock;
          } else {
            plan.blocks.push(updatedBlock);
          }
        } else if (payloadData && payloadData.blockId && payloadData.text !== undefined) {
          blockId = payloadData.blockId;
          newText = payloadData.text;
          const idx = plan.blocks.findIndex(b => b.id === payloadData.blockId);
          if (idx !== -1) {
            oldText = plan.blocks[idx].text;
            blockType = plan.blocks[idx].type;
            plan.blocks[idx].text = payloadData.text;
            if (payloadData.spans) plan.blocks[idx].spans = payloadData.spans;
            if (payloadData.isChecked !== undefined) plan.blocks[idx].isChecked = payloadData.isChecked;
          }
        }

        let action: ChangeAction = 'REPLACE';
        let desc = '';

        if (oldText === newText) {
          action = 'FORMAT';
          desc = `Updated block formatting`;
        } else if (newText.startsWith(oldText) && newText.length > oldText.length) {
          action = 'INSERT';
          const added = newText.substring(oldText.length);
          desc = `Added "${added.length > 25 ? added.substring(0, 25) + '...' : added}"`;
        } else if (oldText.startsWith(newText) && oldText.length > newText.length) {
          action = 'DELETE';
          const removed = oldText.substring(newText.length);
          desc = `Deleted "${removed.length > 25 ? removed.substring(0, 25) + '...' : removed}"`;
        } else {
          action = 'REPLACE';
          desc = `Changed text in ${blockType.toLowerCase()}`;
        }

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action,
          blockId,
          blockType,
          oldContent: oldText,
          newContent: newText,
          description: desc,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      case 'BLOCK_INSERT': {
        let blockId = '';
        let newContent = '';
        let blockType = 'PARAGRAPH';

        if (payloadData && payloadData.block) {
          const newBlock = payloadData.block as EditorBlock;
          blockId = newBlock.id;
          newContent = newBlock.text;
          blockType = newBlock.type;
          const insertIndex = payloadData.targetIndex !== undefined
            ? payloadData.targetIndex
            : payloadData.index !== undefined
              ? payloadData.index
              : plan.blocks.length;
          const safeIndex = Math.max(0, Math.min(insertIndex, plan.blocks.length));
          plan.blocks.splice(safeIndex, 0, newBlock);
        }

        const typeLabel = blockType === 'CHECKLIST' ? 'checklist item' : blockType === 'DIVIDER' ? 'divider' : blockType.toLowerCase();
        const desc = newContent.trim().length > 0
          ? `Created ${typeLabel}: "${newContent.length > 25 ? newContent.substring(0, 25) + '...' : newContent}"`
          : `Added new ${typeLabel}`;

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action: 'BLOCK_CREATE',
          blockId,
          blockType,
          oldContent: '',
          newContent,
          description: desc,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      case 'BLOCK_DELETE': {
        let blockId = '';
        let oldContent = '';
        let blockType = 'PARAGRAPH';

        if (payloadData && payloadData.blockId) {
          blockId = payloadData.blockId;
          const target = plan.blocks.find(b => b.id === blockId);
          if (target) {
            oldContent = target.text;
            blockType = target.type;
          }
          plan.blocks = plan.blocks.filter(b => b.id !== payloadData.blockId);
          if (plan.blocks.length === 0) {
            plan.blocks.push({
              id: 'initial_block',
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
            });
          }
        }

        const desc = oldContent.trim().length > 0
          ? `Deleted block: "${oldContent.length > 25 ? oldContent.substring(0, 25) + '...' : oldContent}"`
          : `Deleted ${blockType.toLowerCase()}`;

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action: 'BLOCK_DELETE',
          blockId,
          blockType,
          oldContent,
          newContent: '',
          description: desc,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      case 'BLOCK_FORMAT': {
        let blockId = '';
        let blockType = 'PARAGRAPH';
        let desc = 'Updated formatting';
        let action: ChangeAction = 'FORMAT';
        let oldVal = '';
        let newVal = '';

        if (payloadData && (payloadData.blockId || payloadData.block?.id)) {
          blockId = payloadData.blockId || payloadData.block.id;
          const idx = plan.blocks.findIndex(b => b.id === blockId);
          if (idx !== -1) {
            const oldBlock = plan.blocks[idx];
            blockType = oldBlock.type;

            if (payloadData.block) {
              const formatted = payloadData.block as EditorBlock;
              if (oldBlock.isChecked !== formatted.isChecked && formatted.type === 'CHECKLIST') {
                action = 'CHECKLIST_UPDATE';
                desc = formatted.isChecked
                  ? `Completed checklist: "${formatted.text.length > 20 ? formatted.text.substring(0, 20) + '...' : formatted.text}"`
                  : `Marked incomplete: "${formatted.text.length > 20 ? formatted.text.substring(0, 20) + '...' : formatted.text}"`;
                oldVal = oldBlock.isChecked ? 'Completed' : 'Pending';
                newVal = formatted.isChecked ? 'Completed' : 'Pending';
              } else if (oldBlock.headingLevel !== formatted.headingLevel) {
                desc = `Changed heading level to ${formatted.headingLevel}`;
                oldVal = oldBlock.headingLevel;
                newVal = formatted.headingLevel;
              } else if (oldBlock.type !== formatted.type) {
                desc = `Changed block type to ${formatted.type.toLowerCase()}`;
                oldVal = oldBlock.type;
                newVal = formatted.type;
              } else if (oldBlock.alignment !== formatted.alignment) {
                desc = `Changed alignment to ${formatted.alignment.toLowerCase()}`;
                oldVal = oldBlock.alignment;
                newVal = formatted.alignment;
              } else {
                desc = `Updated styling on ${blockType.toLowerCase()}`;
              }
              plan.blocks[idx] = formatted;
            } else {
              if (payloadData.isChecked !== undefined && payloadData.isChecked !== oldBlock.isChecked) {
                action = 'CHECKLIST_UPDATE';
                desc = payloadData.isChecked
                  ? `Completed checklist: "${oldBlock.text.length > 20 ? oldBlock.text.substring(0, 20) + '...' : oldBlock.text}"`
                  : `Marked incomplete: "${oldBlock.text.length > 20 ? oldBlock.text.substring(0, 20) + '...' : oldBlock.text}"`;
                plan.blocks[idx].isChecked = payloadData.isChecked;
              }
              if (payloadData.headingLevel) {
                desc = `Changed heading level to ${payloadData.headingLevel}`;
                plan.blocks[idx].headingLevel = payloadData.headingLevel;
              }
              if (payloadData.alignment) {
                desc = `Changed alignment to ${payloadData.alignment.toLowerCase()}`;
                plan.blocks[idx].alignment = payloadData.alignment;
              }
              if (payloadData.type) {
                desc = `Changed block type to ${payloadData.type.toLowerCase()}`;
                plan.blocks[idx].type = payloadData.type;
              }
              if (payloadData.spans) plan.blocks[idx].spans = payloadData.spans;
            }
          }
        }

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action,
          blockId,
          blockType,
          oldContent: oldVal,
          newContent: newVal,
          description: desc,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      case 'RENAME': {
        const oldTitle = plan.title;
        let newTitle = oldTitle;
        if (payloadData && payloadData.title) {
          newTitle = payloadData.title.trim();
          plan.title = newTitle;
        }

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action: 'RENAME',
          oldContent: oldTitle,
          newContent: newTitle,
          description: `Renamed plan to "${newTitle}"`,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      case 'FULL_SYNC': {
        if (payloadData && Array.isArray(payloadData.blocks)) {
          plan.blocks = payloadData.blocks;
        }
        if (payloadData && payloadData.title) {
          plan.title = payloadData.title;
        }
        if (payloadData && payloadData.description !== undefined) {
          plan.description = payloadData.description;
        }

        return {
          changeId,
          operationId: operation.operationId,
          planId: plan.planId,
          userId,
          userName,
          action: 'REPLACE',
          description: `Synced document changes`,
          timestamp,
          revision: plan.revision + 1,
          isAcknowledged: false
        };
      }

      default:
        return null;
    }
  }
}
