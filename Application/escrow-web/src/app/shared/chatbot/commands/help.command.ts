// commands/help.command.ts
import { BaseCommand } from './base-command';
import { CommandContext } from '../models/chat.models';

export class HelpCommand extends BaseCommand {
  name = 'help';
  pattern: RegExp = /^(help|\?|commands)$/i;
  aliases = ['?', 'commands'];
  description = 'Show this help menu.';

  async execute(context: CommandContext, input: string): Promise<string> {
    const commands = context.commands;
    const otherCommands = commands.filter(cmd => cmd.name !== this.name);
    const sorted = [...otherCommands].sort((a, b) => a.name.localeCompare(b.name));

    let html = `<b>🤖 EscrowX Assistant — Available Commands</b><br><br>`;
    sorted.forEach(cmd => {
      const aliases = cmd.aliases.length ? ` (or: ${cmd.aliases.join(', ')})` : '';
      html += `• <code>${cmd.name}</code>${aliases} – ${cmd.description}<br>`;
    });
    html += `<br><i>Type a command to get started. For commands that need an ID, use: <code>command &lt;id&gt;</code></i>`;
    return html;
  }
}