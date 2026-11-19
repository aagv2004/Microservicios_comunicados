import { afterNextRender, Component, computed, inject, Injector, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ForumService } from '../../core/services/forum.service';
import { AuthService } from '../../core/services/auth.service';
import { ModerationItem } from '../../core/models/forum';
import { trimmedLength } from './forum.validators';
@Component({
  selector: 'app-moderation-page',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './moderation-page.html',
  styleUrl: './conversation.css',
})
export class ModerationPage {
  protected readonly forum = inject(ForumService);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly injector = inject(Injector);
  private readonly document = inject(DOCUMENT, { optional: true });
  protected readonly filter = signal('all');
  protected readonly type = signal('all');
  protected readonly items = computed(() =>
    this.auth.user()?.rol === 'ADMIN'
      ? this.forum
          .moderationItems()
          .filter(
            (item) =>
              (this.filter() === 'all' || item.blocked === (this.filter() === 'blocked')) &&
              (this.type() === 'all' || item.type === this.type()),
          )
      : [],
  );
  protected readonly selected = signal<ModerationItem | null>(null);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly form = this.fb.group({ reason: ['', trimmedLength(1, 500)] });
  constructor() {
    const route = inject(ActivatedRoute, { optional: true });
    const params = route?.snapshot.queryParamMap;
    const type = params?.get('tipo');
    const id = Number(params?.get('id'));
    if (
      (type === 'topic' || type === 'comment') &&
      Number.isSafeInteger(id) &&
      id > 0 &&
      this.auth.user()?.rol === 'ADMIN'
    ) {
      this.type.set(type);
      const item = this.forum
        .moderationItems()
        .find((item) => item.type === type && item.id === id);
      if (item) this.select(item);
    }
  }
  protected select(item: ModerationItem): void {
    this.selected.set(item);
    this.form.reset();
    this.error.set('');
    this.message.set('');
    const document = this.document;
    if (!document) return;
    afterNextRender(
      () => {
        const dialog = document.getElementById('moderation-decision') as HTMLDialogElement | null;
        if (this.selected() && dialog && !dialog.open) dialog.showModal();
      },
      { injector: this.injector },
    );
  }
  protected cancel(): void {
    const dialog = this.document?.getElementById('moderation-decision') as HTMLDialogElement | null;
    dialog?.close();
    this.selected.set(null);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const item = this.selected();
    if (!item) return;
    try {
      this.forum.moderate(item.type, item.id, !item.blocked, this.form.getRawValue().reason);
      this.message.set(item.blocked ? 'Contenido restaurado.' : 'Contenido bloqueado.');
      this.cancel();
      this.error.set('');
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo moderar el contenido.');
    }
  }
}
