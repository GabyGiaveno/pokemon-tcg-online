import { LoginRequest, RegisterRequest } from '../models/auth-api.types';
import { AuthFormState, RegisterFormState } from '../models/auth-form.types';

export function mapLoginFormToRequest(form: AuthFormState): LoginRequest {
  return {
    username: form.identifier,
    password: form.password
  };
}

export function mapRegisterFormToRequest(form: RegisterFormState): RegisterRequest {
  return {
    username: form.username,
    email: form.email,
    password: form.password
  };
}
