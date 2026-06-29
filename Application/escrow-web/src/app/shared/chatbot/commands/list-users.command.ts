// commands/list-users.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';
import { firstValueFrom } from 'rxjs';

export class ListUsersCommand extends BaseCommand {
  name = 'list users';
  pattern = /^(?:list users|manage users|users)$/i;
  aliases = ['manage users', 'users'];
  description = 'List users (first 20) and start user management flow.';

  async execute(context: CommandContext, input: string): Promise<string> {
    try {
      const response = await firstValueFrom(context.apiService.getUsers({ size: 20 }));
      const users = response.content || [];
      if (users.length === 0) return 'No users found.';

      // Store the list in the state and set step
      context.updateState({
        currentStep: 'listing_user',
        selectedData: { users }
      });

      let html = '<b>👥 Users (first 20):</b><br>';
      users.forEach((u: any, idx: number) => {
        html += `${idx+1}. <b>${u.displayName || u.email}</b> (${u.role}) – ${u.status}<br>`;
      });
      html += `<br>Please reply with the <b>number</b> of the user you want to manage.`;
      return html;
    } catch (err: any) {
      return `❌ Failed to fetch users: ${err.message || 'unknown error'}`;
    }
  }
}