// commands/user-by-id.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class UserByIdCommand extends BaseCommand {
  name = 'user';
  aliases: string[] = [];
  description = 'Get detailed user info by ID. Usage: `user <id>`';
  pattern = /^user\s+([a-f0-9-]+)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const match = input.match(this.pattern!);
    if (!match) return '❌ Invalid format. Use `user <id>`.';
    const id = match[1];

    try {
      const response = await firstValueFrom(context.apiService.getUserById(id));
      if (!response?.data) return `User ${id} not found.`;
      return this.formatUserDetail(response.data);
    } catch (err: any) {
      return `❌ Error fetching user: ${err.message || 'unknown error'}`;
    }
  }

  private formatUserDetail(user: any): string {
    // same formatting as in LatestUserCommand
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