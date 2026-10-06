import { InjectionToken } from '@angular/core';

export type AppDataMode = 'mock' | 'api';

export const APP_DATA_MODE = new InjectionToken<AppDataMode>('APP_DATA_MODE');
