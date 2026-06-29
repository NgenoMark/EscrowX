// commands/report.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class ReportCommand extends BaseCommand {
  name = 'report';
  aliases = ['today', 'daily'];
  description = 'Show today\'s report (new users, transactions, revenue, disputes).';
  pattern = /^(report|today|daily)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const { apiService } = context;
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);

    try {
      // Use 'size' instead of 'limit'
      const [stats, transactions, users, disputes] = await Promise.all([
        firstValueFrom(apiService.getDashboardStats()),
        firstValueFrom(apiService.getTransactions({ size: 1000 })),
        firstValueFrom(apiService.getUsers({ page: 0, size: 1 })),
        firstValueFrom(apiService.getDisputes({ size: 100 })) // ✅ changed 'limit' to 'size'
      ]);

      // Extract data from responses
      // stats: ApiResponse -> stats.data
      // transactions: PageResponse<ApiEscrowTransaction> -> transactions.content
      // users: PageResponse<ApiUserDetails> -> users.content
      // disputes: PageResponse<any> -> disputes.content

      const statsData = stats?.data || {};
      const transactionsArray = transactions?.content || [];
      const usersArray = users?.content || [];
      const disputesArray = disputes?.content || [];

      // Filter transactions for today
      const todayTxs = transactionsArray.filter((tx: any) => {
        const created = new Date(tx.createdAt);
        return created >= start && created < end;
      });

      const completedToday = todayTxs.filter((tx: any) => tx.status === 'COMPLETED');
      const revenueToday = completedToday.reduce((sum: number, tx: any) => sum + tx.amount, 0);

      // Filter new users today
      const newUsersToday = usersArray.filter((u: any) => {
        const created = new Date(u.createdAt);
        return created >= start && created < end;
      });

      // Open disputes
      const openDisputes = disputesArray.filter((d: any) => d.status === 'OPEN');

      const latestUser = usersArray[0];

      let html = `<b>📊 Today's Report (${today.toLocaleDateString()})</b><br>`;
      html += `<div class="stats-grid">`;
      html += `<div class="stat-item"><span class="stat-label">New Users Today</span><span class="stat-value">${newUsersToday.length}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Transactions Today</span><span class="stat-value">${todayTxs.length}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Revenue Today</span><span class="stat-value">KES ${revenueToday.toLocaleString()}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Open Disputes</span><span class="stat-value">${openDisputes.length}</span></div>`;
      html += `<div class="stat-item"><span class="stat-label">Pending Escrows</span><span class="stat-value">${statsData.pendingEscrows || 'N/A'}</span></div>`;
      if (latestUser) {
        html += `<div class="stat-item"><span class="stat-label">Latest User</span><span class="stat-value">${latestUser.displayName || latestUser.email}</span></div>`;
      }
      html += `</div>`;
      return html;
    } catch (err: any) {
      return `❌ Failed to generate report: ${err.message || 'unknown error'}`;
    }
  }
}