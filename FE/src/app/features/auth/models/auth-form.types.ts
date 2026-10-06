export interface AuthFormState {
  identifier: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterFormState {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  //avatarUrl: string;
}
