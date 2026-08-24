export interface UserProfile {
  userId: string;
  displayName: string;
  avatarPlaceholder?: string;
  createdAt: number;
}

export interface UserSession {
  token: string;
  user: UserProfile;
  expiresAt: number;
}

export interface RichSpan {
  start: number;
  end: number;
  isBold?: boolean;
  isItalic?: boolean;
  isUnderline?: boolean;
  isStrikethrough?: boolean;
  textColorHex?: string;
  highlightColorHex?: string;
  fontSizeSp?: number;
}

export interface EditorBlock {
  id: string;
  type: 'PARAGRAPH' | 'HEADING' | 'BULLET_LIST' | 'NUMBERED_LIST' | 'CHECKLIST' | 'QUOTE' | 'DIVIDER';
  text: string;
  spans: RichSpan[];
  headingLevel: 'NORMAL' | 'H1' | 'H2' | 'H3' | 'H4' | 'H5';
  alignment: 'LEFT' | 'CENTER' | 'RIGHT' | 'JUSTIFY';
  isChecked: boolean;
  numberIndex: number;
  lineSpacing: number;
  letterSpacing: number;
  paragraphSpacingDp: number;
}

export interface SharedPlan {
  planId: string;
  title: string;
  description: string;
  inviteCode: string;
  ownerId: string;
  ownerName: string;
  partnerId: string | null;
  partnerName: string | null;
  revision: number;
  blocks: EditorBlock[];
  createdAt: number;
  updatedAt: number;
}

export type ChangeAction =
  | 'INSERT'
  | 'DELETE'
  | 'REPLACE'
  | 'FORMAT'
  | 'BLOCK_CREATE'
  | 'BLOCK_DELETE'
  | 'CHECKLIST_UPDATE'
  | 'RENAME';

export interface ChangeRecord {
  changeId: string;
  operationId: string;
  planId: string;
  userId: string;
  userName: string;
  action: ChangeAction;
  blockId?: string;
  blockType?: string;
  oldContent?: string;
  newContent?: string;
  description: string;
  timestamp: number;
  revision: number;
  isAcknowledged?: boolean;
}

export type OperationType =
  | 'BLOCK_UPDATE'
  | 'BLOCK_INSERT'
  | 'BLOCK_DELETE'
  | 'BLOCK_FORMAT'
  | 'RENAME'
  | 'FULL_SYNC';

export interface CollaborationOperation {
  operationId: string;
  planId: string;
  userId: string;
  userName?: string;
  type: OperationType;
  revision: number;
  payload: string; // JSON encoded payload for specific op
  timestamp: number;
}

// WebSocket Message Types
export type MessageType =
  | 'AUTH'
  | 'AUTH_ACK'
  | 'JOIN_ROOM'
  | 'JOIN_ACK'
  | 'LEAVE_ROOM'
  | 'OPERATION'
  | 'OPERATION_ACK'
  | 'REMOTE_OPERATION'
  | 'CHANGE_EVENT'
  | 'PRESENCE'
  | 'TYPING'
  | 'ERROR';

export interface WsMessage {
  type: MessageType;
  token?: string;
  planId?: string;
  userId?: string;
  userName?: string;
  operation?: CollaborationOperation;
  changeRecord?: ChangeRecord;
  document?: {
    planId: string;
    title: string;
    description: string;
    revision: number;
    blocks: EditorBlock[];
    ownerId: string;
    ownerName: string;
    partnerId: string | null;
    partnerName: string | null;
    inviteCode: string;
  };
  presence?: {
    userId: string;
    userName: string;
    status: 'ONLINE' | 'OFFLINE';
    isPartner: boolean;
  };
  typing?: {
    userId: string;
    userName: string;
    isTyping: boolean;
  };
  error?: string;
  success?: boolean;
}
