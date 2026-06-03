import { Injectable, signal } from '@angular/core';

export type SearchMode = 'current' | 'global';
export type GlobalSearchFilter = 'all' | 'users' | 'transactions' | 'disputes' | 'audit';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  query = signal('');
  mode = signal<SearchMode>('current');
  globalFilter = signal<GlobalSearchFilter>('all');

  setQuery(value: string): void {
    this.query.set(value.trimStart());
  }

  setMode(mode: SearchMode): void {
    this.mode.set(mode);
  }

  setGlobalFilter(filter: GlobalSearchFilter): void {
    this.globalFilter.set(filter);
  }

  clear(): void {
    this.query.set('');
  }
}
