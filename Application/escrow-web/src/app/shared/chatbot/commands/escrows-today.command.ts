// commands/escrows-today.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class EscrowsTodayCommand extends BaseCommand {
  name = 'escrows today';
  aliases = ['how many escrows today', 'today escrow count'];
  pattern = /^escrows today$/i;
  description = 'Count how many escrows were created today.';

  async execute(context: CommandContext, input: string): Promise<string> {
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);

    try {
      const response = await firstValueFrom(context.apiService.getTransactions({ size: 1000 }));
      const all = response.content || [];
      const todayCount = all.filter((tx: any) => {
        const created = new Date(tx.createdAt);
        return created >= start && created < end;
      }).length;
      return `📆 There were <b>${todayCount}</b> escrow transactions created today.`;
    } catch (err: any) {
      return `❌ Failed to count escrows: ${err.message || 'unknown error'}`;
    }
  }
}