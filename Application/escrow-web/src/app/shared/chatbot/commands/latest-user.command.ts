// commands/latest-user.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class LatestUserCommand extends BaseCommand {
  pattern = /^latest user$/i;
  name = 'latest user';
  aliases = ['newest user'];
  description = 'Show the newest registered user.';

  async execute(context: CommandContext, input: string): Promise<string> {
    try {
      const response = await firstValueFrom(context.apiService.getUsers({ page: 0, size: 1 }));
      const user = response.content?.[0];
      if (!user) return 'No users found.';
      return this.formatUserDetail(user);
    } catch (err: any) {
      return `❌ Failed to fetch latest user: ${err.message || 'unknown error'}`;
    }
  }

  private formatUserDetail(user: any): string {
    return `
<div class="detail-card">
  <div><span class="label">ID:</span> ${user.id}</div>
  <div><span class="label">Name:</span> ${user.displayName || 'N/A'}</div>
  <div><span class="label">Email:</span> ${user.email}</div>
  <div><span class="label">Phone:</span> ${user.phone}</div>
  <div><span class="label">Role:</span> ${user.role}</div>
  <div><span class="label">Status:</span> ${user.status}</div>
  <div><span class="label">Business:</span> ${user.businessName || 'N/A'}</div>
  <div><span class="label">Created:</span> ${this.formatDate(user.createdAt)}</div>
</div>
    `;
  }
}