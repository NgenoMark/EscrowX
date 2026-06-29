// commands/activate-user.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class ActivateUserCommand extends BaseCommand {
  name = 'activate';
  aliases: string[] = [];
  description = 'Activate a user by ID. Usage: `activate <userId>`';
  pattern = /^activate\s+([a-f0-9-]+)$/i;

  async execute(context: CommandContext, input: string): Promise<string> {
    const match = input.match(this.pattern!);
    if (!match) return '❌ Invalid format. Use `activate <userId>`.';
    const userId = match[1];

    try {
      await firstValueFrom(context.apiService.activateUser(userId));
      return `✅ User ${userId} activated.`;
    } catch (err: any) {
      return `❌ Failed to activate user: ${err.message || 'unknown error'}`;
    }
  }
}