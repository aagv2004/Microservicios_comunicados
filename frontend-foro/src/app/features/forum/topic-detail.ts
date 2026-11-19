import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ForumService } from '../../core/services/forum.service';
import { AuthService } from '../../core/services/auth.service';
import { trimmedLength } from './forum.validators';
@Component({
  selector: 'app-topic-detail',
  imports: [RouterLink, ReactiveFormsModule, DatePipe],
  templateUrl: './topic-detail.html',
  styleUrl: './conversation.css',
})
export class TopicDetail {
  protected readonly forum = inject(ForumService);
  protected readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly params = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly id = computed(() => Number(this.params().get('id')));
  protected readonly topic = computed(() => this.forum.topic(this.id()));
  protected readonly comments = computed(() => this.forum.comments(this.id()));
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly editingComment = signal<number | null>(null);
  protected readonly deleting = signal<{ type: 'topic' | 'comment'; id: number } | null>(null);
  protected readonly form = this.fb.group({ content: ['', trimmedLength(1, 5000)] });
  protected readonly editForm = this.fb.group({ content: ['', trimmedLength(1, 5000)] });
  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.act(() => {
      this.forum.comment(this.id(), this.form.getRawValue().content);
      this.form.reset();
    }, 'Comentario publicado.');
  }
  protected startEdit(id: number, content: string): void {
    this.editingComment.set(id);
    this.editForm.reset({ content });
    this.error.set('');
  }
  protected saveComment(): void {
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) return;
    const id = this.editingComment();
    if (id !== null)
      this.act(() => {
        this.forum.updateComment(id, this.editForm.getRawValue().content);
        this.editingComment.set(null);
      }, 'Comentario actualizado.');
  }
  protected confirmDelete(): void {
    const target = this.deleting();
    if (!target) return;
    this.deleting.set(null);
    this.act(() => {
      if (target.type === 'topic') {
        this.forum.deleteTopic(target.id);
        void this.router.navigateByUrl('/');
      } else this.forum.deleteComment(target.id);
    }, 'Contenido eliminado.');
  }
  private act(action: () => void, success: string): void {
    this.error.set('');
    this.message.set('');
    try {
      action();
      this.message.set(success);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo completar la acción.');
    }
  }
}
