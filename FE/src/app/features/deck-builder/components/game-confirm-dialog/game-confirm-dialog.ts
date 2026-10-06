import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-game-confirm-dialog',
  imports: [],
  templateUrl: './game-confirm-dialog.html',
  styleUrl: './game-confirm-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameConfirmDialog {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmText = input<string>('CONFIRMAR');
  readonly cancelText = input<string>('CANCELAR');
  readonly danger = input<boolean>(false);

  readonly confirm = output<void>();
  readonly cancel = output<void>();

  protected onConfirm(): void {
    this.confirm.emit();
  }

  protected onCancel(): void {
    this.cancel.emit();
  }

  protected onBackdropClick(): void {
    this.onCancel();
  }
}
