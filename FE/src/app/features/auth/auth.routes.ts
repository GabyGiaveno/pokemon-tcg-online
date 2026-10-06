import { Routes } from '@angular/router';
import { LoginPage } from './login/login';
import { RegisterPage } from './register/register';
import { ForgotPasswordPage } from './forgot-password/forgot-password';
import { ResetPasswordPage } from './reset-password/reset-password';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'forgot-password', component: ForgotPasswordPage },
  { path: 'reset-password', component: ResetPasswordPage },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];
