import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth-background',
  imports: [],
  templateUrl: './auth-background.html',
  styleUrl: './auth-background.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthBackground {
  protected readonly particles = Array.from({ length: 10 }, (_, index) => index);
}
