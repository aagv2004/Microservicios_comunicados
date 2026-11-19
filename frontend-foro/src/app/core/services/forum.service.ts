import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import {
  Category,
  Topic,
  ForumComment,
  TopicInput,
  TopicSummary,
  ModerationItem,
} from '../models/forum';

@Injectable({ providedIn: 'root' })
export class ForumService {
  private readonly auth = inject(AuthService);
  readonly categories: readonly Category[] = [
    { id: 'tecnologia', name: 'Tecnología', symbol: '⌘' },
    { id: 'videojuegos', name: 'Videojuegos', symbol: '✳' },
    { id: 'aprendizaje', name: 'Aprendizaje', symbol: '↗' },
    { id: 'creatividad', name: 'Creatividad', symbol: '◈' },
    { id: 'comunidad', name: 'Comunidad', symbol: '◎' },
  ];
  private readonly topicState = signal<readonly Topic[]>([
    this.seedTopic(
      1,
      'tecnologia',
      '¿Qué herramienta cambió tu forma de programar?',
      'Desde un atajo del editor hasta esa extensión que ya no puedes dejar de usar. Compartamos nuestros descubrimientos.',
      'Camila Rojas',
    ),
    this.seedTopic(
      2,
      'videojuegos',
      'Jugar para mejorar: ¿cómo revisan sus partidas?',
      'Estoy empezando a revisar mis repeticiones. ¿En qué se fijan primero para aprender sin perderse en los detalles?',
      'Diego Muñoz',
    ),
    this.seedTopic(
      3,
      'aprendizaje',
      'Pequeños proyectos, grandes aprendizajes',
      'Un espacio para mostrar lo que estamos construyendo y contar qué aprendimos en el camino.',
      'Javiera Soto',
    ),
    this.seedTopic(
      4,
      'creatividad',
      'Inspiración para diseñar una interfaz más simple',
      'Menos elementos, más intención. ¿Qué sitios o aplicaciones les inspiran cuando comienzan un diseño?',
      'Matías Vera',
    ),
    this.seedTopic(
      5,
      'comunidad',
      'Preséntate: ¿qué te gustaría aprender este mes?',
      'Cuéntanos un poco de ti, tus intereses y esa idea que tienes pendiente. Quizás encuentres con quién compartirla.',
      'Antonia Díaz',
    ),
  ]);
  private readonly commentState = signal<readonly ForumComment[]>([]);
  private nextTopicId = 6;
  private nextCommentId = 1;
  readonly topics = computed<readonly TopicSummary[]>(() =>
    this.topicState()
      .filter((topic) => !topic.blocked)
      .map((topic) => ({
        ...topic,
        author: this.authorName(topic),
        excerpt: topic.content.slice(0, 180),
        initials: this.authorName(topic)
          .split(/\s+/)
          .map((word) => word[0])
          .slice(0, 2)
          .join(''),
        replies: this.commentState().filter(
          (comment) => comment.topicId === topic.id && !comment.blocked,
        ).length,
      }))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id),
  );

  private seedTopic(
    id: number,
    categoryId: string,
    title: string,
    content: string,
    author: string,
  ): Topic {
    const date = new Date(Date.UTC(2026, 10, 19 - id, 12)).toISOString();
    return {
      id,
      categoryId,
      title,
      content,
      author,
      authorId: 'seed-' + id,
      createdAt: date,
      updatedAt: date,
      blocked: false,
      moderationReason: '',
      moderatorId: null,
    };
  }
  private authorName(item: { authorId: string; author: string }): string {
    const user = this.auth.user();
    return user?.id === item.authorId ? user.nombre : item.author;
  }
  categoryName(id: string): string {
    return this.categories.find((category) => category.id === id)?.name ?? '';
  }
  topic(id: number): Topic | undefined {
    const topic = this.topicState().find((item) => item.id === id && !item.blocked);
    return topic ? { ...topic, author: this.authorName(topic) } : undefined;
  }
  comments(topicId: number): readonly ForumComment[] {
    if (!this.topic(topicId)) return [];
    return this.commentState()
      .filter((comment) => comment.topicId === topicId && !comment.blocked)
      .map((comment) => ({ ...comment, author: this.authorName(comment) }));
  }
  myTopics(): readonly TopicSummary[] {
    const user = this.auth.user();
    if (!user) return [];
    return this.topicState()
      .filter((topic) => topic.authorId === user.id)
      .map((topic) => ({
        ...topic,
        author: user.nombre,
        excerpt: topic.content.slice(0, 180),
        initials: user.nombre
          .split(/\s+/)
          .map((word) => word[0])
          .slice(0, 2)
          .join(''),
        replies: this.commentState().filter(
          (comment) => comment.topicId === topic.id && !comment.blocked,
        ).length,
      }))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id);
  }

  canEdit(item: { authorId: string; blocked: boolean }): boolean {
    const user = this.auth.user();
    return !!user && user.id === item.authorId && !item.blocked;
  }
  private requireUser() {
    const user = this.auth.user();
    if (!user) throw new Error('Inicia sesión para realizar esta acción.');
    return user;
  }
  private requireAuthor(item: { authorId: string; blocked: boolean }): void {
    if (!this.canEdit(item)) throw new Error('Solo el autor puede modificar contenido disponible.');
  }
  private requireAdmin() {
    const user = this.requireUser();
    if (user.rol !== 'ADMIN') throw new Error('Esta acción requiere el rol ADMIN.');
    return user;
  }
  private requireTopic(id: number): Topic {
    const topic = this.topic(id);
    if (!topic) throw new Error('La conversación no está disponible.');
    return topic;
  }
  private validateContent(content: string, maximum: number): string {
    const trimmed = content.trim();
    if (!trimmed || trimmed.length > maximum)
      throw new Error(`El contenido debe tener entre 1 y ${maximum} caracteres.`);
    return trimmed;
  }
  private validateTopic(input: TopicInput): TopicInput {
    const title = input.title.trim();
    if (title.length < 3 || title.length > 150)
      throw new Error('El título debe tener entre 3 y 150 caracteres.');
    if (!this.categories.some((category) => category.id === input.categoryId))
      throw new Error('Selecciona una categoría válida.');
    return {
      title,
      content: this.validateContent(input.content, 10000),
      categoryId: input.categoryId,
    };
  }
  publish(input: TopicInput): number {
    const user = this.requireUser();
    const values = this.validateTopic(input);
    const id = this.nextTopicId++;
    const now = new Date().toISOString();
    this.topicState.update((items) => [
      ...items,
      {
        ...values,
        id,
        authorId: user.id,
        author: user.nombre,
        createdAt: now,
        updatedAt: now,
        blocked: false,
        moderationReason: '',
        moderatorId: null,
      },
    ]);
    return id;
  }
  updateTopic(id: number, input: TopicInput): void {
    const topic = this.requireTopic(id);
    this.requireAuthor(topic);
    const values = this.validateTopic(input);
    this.topicState.update((items) =>
      items.map((item) =>
        item.id === id ? { ...item, ...values, updatedAt: new Date().toISOString() } : item,
      ),
    );
  }
  deleteTopic(id: number): void {
    const topic = this.requireTopic(id);
    this.requireAuthor(topic);
    if (this.commentState().some((comment) => comment.topicId === id))
      throw new Error(
        'Este tema tiene comentarios. Deben eliminarse antes de borrar la conversación.',
      );
    this.topicState.update((items) => items.filter((item) => item.id !== id));
  }
  comment(topicId: number, content: string): number {
    const user = this.requireUser();
    this.requireTopic(topicId);
    const text = this.validateContent(content, 5000);
    const id = this.nextCommentId++;
    const now = new Date().toISOString();
    this.commentState.update((items) => [
      ...items,
      {
        id,
        topicId,
        content: text,
        authorId: user.id,
        author: user.nombre,
        createdAt: now,
        updatedAt: now,
        blocked: false,
        moderationReason: '',
        moderatorId: null,
      },
    ]);
    return id;
  }
  updateComment(id: number, content: string): void {
    const comment = this.commentState().find((item) => item.id === id);
    if (!comment) throw new Error('El comentario no está disponible.');
    this.requireTopic(comment.topicId);
    this.requireAuthor(comment);
    const text = this.validateContent(content, 5000);
    this.commentState.update((items) =>
      items.map((item) =>
        item.id === id ? { ...item, content: text, updatedAt: new Date().toISOString() } : item,
      ),
    );
  }
  deleteComment(id: number): void {
    const comment = this.commentState().find((item) => item.id === id);
    if (!comment) throw new Error('El comentario no está disponible.');
    this.requireTopic(comment.topicId);
    this.requireAuthor(comment);
    this.commentState.update((items) => items.filter((item) => item.id !== id));
  }
  moderationItems(): readonly ModerationItem[] {
    this.requireAdmin();
    return [
      ...this.topicState().map((topic) => ({
        type: 'topic' as const,
        id: topic.id,
        topicId: topic.id,
        title: topic.title,
        author: this.authorName(topic),
        blocked: topic.blocked,
        reason: topic.moderationReason,
        parentBlocked: false,
      })),
      ...this.commentState().map((comment) => ({
        type: 'comment' as const,
        id: comment.id,
        topicId: comment.topicId,
        title: comment.content,
        author: this.authorName(comment),
        blocked: comment.blocked,
        reason: comment.moderationReason,
        parentBlocked: this.topicState().some(
          (topic) => topic.id === comment.topicId && topic.blocked,
        ),
      })),
    ];
  }
  moderate(type: 'topic' | 'comment', id: number, blocked: boolean, reason: string): void {
    const admin = this.requireAdmin();
    const text = this.validateContent(reason, 500);
    const patch = {
      blocked,
      moderationReason: text,
      moderatorId: admin.id,
      updatedAt: new Date().toISOString(),
    };
    if (type === 'topic') {
      if (!this.topicState().some((topic) => topic.id === id))
        throw new Error('La conversación no existe.');
      this.topicState.update((items) =>
        items.map((item) => (item.id === id ? { ...item, ...patch } : item)),
      );
    } else {
      if (!this.commentState().some((comment) => comment.id === id))
        throw new Error('El comentario no existe.');
      this.commentState.update((items) =>
        items.map((item) => (item.id === id ? { ...item, ...patch } : item)),
      );
    }
  }
}
