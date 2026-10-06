import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AuthBackground } from '../auth-background/auth-background';
import { AuthLogo } from '../auth-logo/auth-logo';

@Component({
  selector: 'app-auth-shell',
  imports: [AuthBackground, AuthLogo],
  templateUrl: './auth-shell.html',
  styleUrl: './auth-shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthShell {}
