import { LoginRequest, PlayerResponse, RegisterRequest } from '../models/auth-api.types';

export interface MockAuthUser {
  id: number;
  username: string;
  email: string;
  password: string;
}

const USERS_KEY = 'mock_auth_users';

const DEFAULT_USERS: MockAuthUser[] = [
  { id: 1, username: 'ash', email: 'ash@example.com', password: 'pikachu123' },
  { id: 2, username: 'misty', email: 'misty@example.com', password: 'starmie123' },
];

export function loadMockUsers(): MockAuthUser[] {
  const raw = localStorage.getItem(USERS_KEY);
  if (!raw) {
    saveMockUsers(DEFAULT_USERS);
    return [...DEFAULT_USERS];
  }

  try {
    const users = JSON.parse(raw) as MockAuthUser[];
    return Array.isArray(users) && users.length > 0 ? users : [...DEFAULT_USERS];
  } catch {
    saveMockUsers(DEFAULT_USERS);
    return [...DEFAULT_USERS];
  }
}

export function saveMockUsers(users: MockAuthUser[]): void {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

export function findMockUser(identifier: string): MockAuthUser | undefined {
  const normalized = identifier.trim().toLowerCase();
  return loadMockUsers().find(
    (user) => user.username.toLowerCase() === normalized || user.email.toLowerCase() === normalized,
  );
}

export function registerMockUser(request: RegisterRequest): PlayerResponse {
  const users = loadMockUsers();
  const usernameTaken = users.some((user) => user.username.toLowerCase() === request.username.toLowerCase());
  const emailTaken = users.some((user) => user.email.toLowerCase() === request.email.toLowerCase());

  if (usernameTaken) {
    throw new Error('Username already exists');
  }

  if (emailTaken) {
    throw new Error('Email already exists');
  }

  const nextUser: MockAuthUser = {
    id: Math.max(0, ...users.map((user) => user.id)) + 1,
    username: request.username,
    email: request.email,
    password: request.password,
  };

  saveMockUsers([...users, nextUser]);
  return toPlayerResponse(nextUser);
}

export function loginMockUser(request: LoginRequest): PlayerResponse {
  const user = findMockUser(request.username);
  if (!user || user.password !== request.password) {
    throw new Error('Invalid credentials');
  }

  return toPlayerResponse(user);
}

export function updateMockPassword(token: string, newPassword: string): string {
  const email = localStorage.getItem(`mock_reset_token_${token}`);
  if (!email) {
    throw new Error('Invalid or expired token');
  }

  const users = loadMockUsers();
  const index = users.findIndex((user) => user.email.toLowerCase() === email.toLowerCase());
  if (index === -1) {
    throw new Error('Invalid or expired token');
  }

  users[index] = { ...users[index], password: newPassword };
  saveMockUsers(users);
  localStorage.removeItem(`mock_reset_token_${token}`);
  return users[index].username;
}

export function createMockResetToken(email: string): string {
  const user = loadMockUsers().find((entry) => entry.email.toLowerCase() === email.toLowerCase());
  if (!user) {
    throw new Error('Email not found');
  }

  const token = `mock-reset-${user.id}-${Date.now()}`;
  localStorage.setItem(`mock_reset_token_${token}`, user.email);
  return token;
}

function toPlayerResponse(user: MockAuthUser): PlayerResponse {
  return {
    id: user.id,
    username: user.username,
    token: `mock-token-${user.id}-${Date.now()}`,
  };
}
