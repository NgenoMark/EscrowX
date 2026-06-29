// commands/blacklist-user.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class BlacklistUserCommand extends BaseCommand {
  name = 'blacklist';
  aliases: string[] = [];
  description = 'Blacklist a user by ID. Usage: `blacklist <userId>`';
  pattern = /^blacklist\s+([a-f0-9-]+)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const match = input.match(this.pattern!);
    if (!match) return '❌ Invalid format. Use `blacklist <userId>`.';
    const userId = match[1];

    try {
      await firstValueFrom(context.apiService.blacklistUser(userId));
      return `✅ User ${userId} blacklisted.`;
    } catch (err: any) {
      return `❌ Failed to blacklist user: ${err.message || 'unknown error'}`;
    }
  }
}