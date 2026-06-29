// commands/list-disputes.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class ListDisputesCommand extends BaseCommand {
  name = 'list disputes';
  aliases = ['manage disputes', 'disputes'];
  pattern = /^(list disputes|manage disputes|disputes)$/i;
  description = 'List disputes (first 20) and start dispute resolution flow.';

  async execute(context: CommandContext, input: string): Promise<string> {
    try {
      const response = await firstValueFrom(context.apiService.getDisputes({ size: 20 }));
      const disputes = response.content || [];
      if (disputes.length === 0) return 'No disputes found.';

      context.updateState({
        currentStep: 'listing_disputes',
        selectedData: { disputes }
      });

      let html = '<b>⚖️ Disputes (first 20):</b><br>';
      disputes.forEach((d: any, idx: number) => {
        html += `${idx+1}. <b>${this.truncateId(d.id)}</b> – ${d.status} | KES ${d.amount} | Raised by: ${d.raisedBy}<br>`;
      });
      html += `<br>Please reply with the <b>number</b> of the dispute you want to resolve.`;
      return html;
    } catch (err: any) {
      return `❌ Failed to fetch disputes: ${err.message || 'unknown error'}`;
    }
  }
}