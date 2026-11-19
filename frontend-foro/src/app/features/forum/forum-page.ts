import { Component, computed, inject, signal } from '@angular/core';
import { ForumService } from '../../core/services/forum.service';
@Component({
  selector: 'app-forum-page',
  templateUrl: './forum-page.html',
  styleUrl: './forum-page.css',
})
export class ForumPage {
  protected readonly forum = inject(ForumService);
  protected readonly category = signal('');
  protected readonly query = signal('');
  protected readonly expanded = signal<number | null>(null);
  protected readonly topics = computed(() => {
    const query = this.normalize(this.query().trim());
    return this.forum.topics.filter(
      (topic) =>
        (!this.category() || topic.categoryId === this.category()) &&
        this.normalize(`${topic.title} ${topic.excerpt} ${topic.author}`).includes(query),
    );
  });
  protected categoryName(id: string): string {
    return this.forum.categories.find((category) => category.id === id)?.name ?? '';
  }
  protected reset(): void {
    this.query.set('');
    this.category.set('');
  }
  private normalize(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }
}
