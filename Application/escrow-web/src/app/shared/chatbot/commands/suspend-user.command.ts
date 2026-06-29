// commands/suspend-user.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class SuspendUserCommand extends BaseCommand {
  name = 'suspend';
  aliases: string[] = [];
  description = 'Suspend a user by ID. Usage: `suspend <userId>`';
  pattern = /^suspend\s+([a-f0-9-]+)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const match = input.match(this.pattern!);
    if (!match) return '❌ Invalid format. Use `suspend <userId>`.';
    const userId = match[1];

    try {
      await firstValueFrom(context.apiService.suspendUser(userId));
      return `✅ User ${userId} suspended.`;
    } catch (err: any) {
      return `❌ Failed to suspend user: ${err.message || 'unknown error'}`;
    }
  }
}