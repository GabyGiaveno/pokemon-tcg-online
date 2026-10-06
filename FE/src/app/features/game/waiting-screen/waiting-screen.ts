import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-waiting-screen',
  templateUrl: './waiting-screen.html',
  styleUrl: './waiting-screen.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
})
export class WaitingScreen {
  readonly gameId = input.required<string>();
}
