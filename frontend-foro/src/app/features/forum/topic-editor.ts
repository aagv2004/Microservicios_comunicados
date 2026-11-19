import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ForumService } from '../../core/services/forum.service';
import { trimmedLength } from './forum.validators';
@Component({
  selector: 'app-topic-editor',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './topic-editor.html',
  styleUrl: './conversation.css',
})
export class TopicEditor {
  protected readonly forum = inject(ForumService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly editing = !!this.route.snapshot.paramMap.get('id');
  protected readonly topic = computed(() => (this.editing ? this.forum.topic(this.id) : undefined));
  protected readonly allowed = computed(
    () => !this.editing || (!!this.topic() && this.forum.canEdit(this.topic()!)),
  );
  protected readonly error = signal('');
  protected readonly form = this.fb.group({
    title: ['', trimmedLength(3, 150)],
    categoryId: ['', Validators.required],
    content: ['', trimmedLength(1, 10000)],
  });
  constructor() {
    const topic = this.topic();
    if (topic)
      this.form.reset({ title: topic.title, content: topic.content, categoryId: topic.categoryId });
  }
  protected submit(): void {
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.error.set('Revisa el título, la categoría y el contenido.');
      return;
    }
    try {
      const values = this.form.getRawValue();
      if (this.editing) this.forum.updateTopic(this.id, values);
      const id = this.editing ? this.id : this.forum.publish(values);
      void this.router.navigate(['/temas', id]);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo guardar el tema.');
    }
  }
}
