import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { NewsItem } from '../models/news.model';

@Injectable({ providedIn: 'root' })
export class NewsService {
  private readonly http = inject(HttpClient);
  private readonly news$ = this.http.get<NewsItem[]>('/data/news.json').pipe(shareReplay(1));

  getNews(): Observable<NewsItem[]> {
    return this.news$;
  }
}
