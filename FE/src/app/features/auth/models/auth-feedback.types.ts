export type AuthFeedbackType = 'success' | 'error' | 'info';

export interface AuthFeedback {
  type: AuthFeedbackType;
  message: string;
}
