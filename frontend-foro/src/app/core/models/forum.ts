export interface Category {
  id: string;
  name: string;
  symbol: string;
}
export interface Topic {
  id: number;
  categoryId: string;
  title: string;
  excerpt: string;
  author: string;
  initials: string;
  replies: number;
}
