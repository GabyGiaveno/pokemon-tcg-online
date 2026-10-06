import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-auth-logo',
  imports: [],
  templateUrl: './auth-logo.html',
  styleUrl: './auth-logo.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthLogo {
  readonly title = input('Pokémon TCG');
  readonly imageSrc = input('/images/pokebola.png');
}
