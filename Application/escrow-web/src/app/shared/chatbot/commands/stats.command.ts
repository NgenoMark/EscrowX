// commands/stats.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class StatsCommand extends BaseCommand {
  pattern = /^(stats|dashboard)\b/i;
  name = 'stats';
  aliases = ['dashboard'];
  description = 'Show dashboard statistics (users, transactions, escrows, volume).';

  async execute(context: CommandContext, input: string): Promise<string> {
    try {
      const response = await firstValueFrom(context.apiService.getDashboardStats());
      const stats = response?.data || {};
      let html = `<div class="stats-grid">`;
      html += `<div class="stat-item"><span class="stat-label">Total Users</span><span class="stat-value">${stats.totalUsers || 'N/A'}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Total Transactions</span><span class="stat-value">${stats.totalTransactions || 'N/A'}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Active Escrows</span><span class="stat-value">${stats.activeEscrows || 'N/A'}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Open Disputes</span><span class="stat-value">${stats.openDisputes || 'N/A'}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Total Volume</span><span class="stat-value">KES ${stats.totalVolume?.toLocaleString() || '0'}</span></div>`;
      html += `</div>`;
      return html;
    } catch (err: any) {
      return `❌ Failed to fetch stats: ${err.message || 'unknown error'}`;
    }
  }
}