export interface Category {
  readonly id: string;
  readonly name: string;
  readonly symbol: string;
}
export interface ModeratedContent {
  readonly id: number;
  readonly authorId: string;
  readonly author: string;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly blocked: boolean;
  readonly moderationReason: string;
  readonly moderatorId: string | null;
}
export interface Topic extends ModeratedContent {
  readonly categoryId: string;
  readonly title: string;
  readonly content: string;
}
export interface ForumComment extends ModeratedContent {
  readonly topicId: number;
  readonly content: string;
}
export interface TopicSummary extends Topic {
  readonly excerpt: string;
  readonly initials: string;
  readonly replies: number;
}
export interface TopicInput {
  title: string;
  content: string;
  categoryId: string;
}
export interface ModerationItem {
  type: 'topic' | 'comment';
  id: number;
  topicId: number;
  title: string;
  author: string;
  blocked: boolean;
  reason: string;
  parentBlocked: boolean;
}
