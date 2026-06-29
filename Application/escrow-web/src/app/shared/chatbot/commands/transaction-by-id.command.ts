// commands/transaction-by-id.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class TransactionByIdCommand extends BaseCommand {
  name = 'tx';
  aliases: string[] = [];
  description = 'Get detailed transaction info by ID. Usage: `tx <id>`';
  pattern = /^tx\s+([a-f0-9-]+)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const match = input.match(this.pattern!);
    if (!match) return '❌ Invalid format. Use `tx <id>`.';
    const id = match[1];

    try {
      const response = await firstValueFrom(context.apiService.getTransactionById(id));
      if (!response?.data) return `Transaction ${id} not found.`;
      return this.formatTransactionDetail(response.data);
    } catch (err: any) {
      return `❌ Error fetching transaction: ${err.message || 'unknown error'}`;
    }
  }

  private formatTransactionDetail(tx: any): string {
    return `
<div class="detail-card">
  <div><span class="label">ID:</span> ${tx.id}</div>
  <div><span class="label">Status:</span> ${tx.status}</div>
  <div><span class="label">Amount:</span> KES ${tx.amount}</div>
  <div><span class="label">Buyer:</span> ${tx.buyerId}</div>
  <div><span class="label">Seller:</span> ${tx.sellerId}</div>
  <div><span class="label">Created:</span> ${this.formatDate(tx.createdAt)}</div>
  <div><span class="label">Auto Release:</span> ${tx.autoReleaseAt ? this.formatDate(tx.autoReleaseAt) : 'N/A'}</div>
</div>
    `;
  }
}