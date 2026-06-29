import { Injectable, inject, signal, Signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { firstValueFrom } from 'rxjs';

import { 
  ChatState, 
  ChatMessage, 
  ChatStep, 
  ChatSender, 
  ChatCommand,
  CommandContext 
} from '../../chatbot/models/chat.models';

// Import all commands
import {
  HelpCommand,
  ReportCommand,
  LatestUserCommand,
  EscrowsTodayCommand,
  ListUsersCommand,
  ListDisputesCommand,
  UserByIdCommand,
  TransactionByIdCommand,
  StatsCommand,
  SuspendUserCommand,
  ActivateUserCommand,
  BlacklistUserCommand
} from '../commands';

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private apiService = inject(ApiService);

  private initialState: ChatState = {
    messages: [{
      text: '👋 Hello! I\'m your EscrowX admin assistant. Try typing "help" to see what I can do.',
      sender: 'assistant',
      timestamp: new Date()
    }],
    isOpen: false,
    isLoading: false,
    inputText: '',
    currentStep: 'idle',
    selectedData: {},
    isStreaming: false
  };

  private stateSignal = signal<ChatState>({ ...this.initialState });

  get state(): Signal<ChatState> {
    return this.stateSignal.asReadonly();
  }

  private commands: ChatCommand[] = [];

  constructor() {
    this.registerCommands();
    // Give each initial message a real id
    const initialMessages = this.initialState.messages.map(m => ({
      ...m,
      id: this.generateId()
    }));
    this.initialState.messages = initialMessages;
    this.stateSignal.set({ ...this.initialState });
  }

  private registerCommands(): void {
    this.commands = [
      new HelpCommand(),
      new ReportCommand(),
      new LatestUserCommand(),
      new EscrowsTodayCommand(),
      new ListUsersCommand(),
      new ListDisputesCommand(),
      new UserByIdCommand(),
      new TransactionByIdCommand(),
      new StatsCommand(),
      new SuspendUserCommand(),
      new ActivateUserCommand(),
      new BlacklistUserCommand()
    ];
  }

  toggleChat(): void {
    this.updateState({ isOpen: !this.stateSignal().isOpen });
    if (!this.stateSignal().isOpen) {
      this.updateState({ currentStep: 'idle', selectedData: {} });
    }
  }

  async sendMessage(input: string): Promise<void> {
    const trimmed = input.trim();
    if (!trimmed) return;
    if (this.stateSignal().isStreaming) return;

    this.addMessage(trimmed, 'user');
    this.updateState({ inputText: '' });
    this.updateState({ isLoading: true });

    try {
      let response: string;
      const step = this.stateSignal().currentStep;
      if (step !== 'idle') {
        response = await this.processStep(trimmed);
      } else {
        response = await this.processCommand(trimmed);
      }
      await this.streamMessage(response);
    } catch (error: any) {
      await this.streamMessage(`❌ Error: ${error.message || 'Something went wrong.'}`);
    } finally {
      this.updateState({ isLoading: false });
      this.updateState({ isStreaming: false });
    }
  }

  addMessage(text: string, sender: ChatSender): void {
    const msg: ChatMessage = {
      id: this.generateId(),
      text,
      sender,
      timestamp: new Date()
    };
    const current = this.stateSignal().messages;
    this.updateState({ messages: [...current, msg] });
  }

  setInputText(text: string): void {
    this.updateState({ inputText: text });
  }

  // ---------- HTML‑aware Streaming ----------
  private generateId(): string {
    return Date.now().toString(36) + '-' + Math.random().toString(36).substr(2, 9);
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private async streamMessage(fullText: string): Promise<void> {
    // If the message has no HTML tags, stream as plain text
    if (!fullText.includes('<')) {
      await this.streamPlainText(fullText);
      return;
    }

    try {
      // 1. Parse the HTML and extract all text nodes
      const parser = new DOMParser();
      const doc = parser.parseFromString(`<div id="root">${fullText}</div>`, 'text/html');
      const root = doc.getElementById('root')!;

      const textNodes: Text[] = [];
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
      let node: Text | null;
      while ((node = walker.nextNode() as Text)) {
        textNodes.push(node);
      }

      // If there are no text nodes, just show the full HTML immediately
      if (textNodes.length === 0) {
        this.addMessage(fullText, 'assistant');
        return;
      }

      // 2. Store original text contents and replace each text node with a placeholder <span>
      const textContents: string[] = textNodes.map(n => n.textContent || '');
      textNodes.forEach((n, idx) => {
        const span = document.createElement('span');
        span.setAttribute('data-index', idx.toString());
        span.textContent = '';
        n.parentNode?.replaceChild(span, n);
      });

      const skeletonHtml = root.innerHTML;

      // 3. Create a new assistant message with the skeleton HTML (empty text)
      const msgId = this.generateId();
      const msg: ChatMessage = {
        id: msgId,
        text: skeletonHtml,
        sender: 'assistant',
        timestamp: new Date()
      };
      const currentMessages = this.stateSignal().messages;
      this.updateState({ messages: [...currentMessages, msg], isStreaming: true });

      // 4. Prepare streaming: flatten all words and map each word to its text node index
      const allWords: string[] = [];
      const wordNodeIndices: number[] = [];
      textContents.forEach((text, idx) => {
        const words = text.split(/\s+/).filter(w => w.length > 0);
        words.forEach(w => {
          allWords.push(w);
          wordNodeIndices.push(idx);
        });
      });

      // 5. Accumulate text per node
      const accumulatedTexts: string[] = textContents.map(() => '');

      // 6. Stream word by word
      for (let i = 0; i < allWords.length; i++) {
        const word = allWords[i];
        const nodeIdx = wordNodeIndices[i];
        if (accumulatedTexts[nodeIdx].length > 0) {
          accumulatedTexts[nodeIdx] += ' ';
        }
        accumulatedTexts[nodeIdx] += word;

        // Rebuild the HTML by filling placeholders with accumulated text
        const tempRoot = document.createElement('div');
        tempRoot.innerHTML = skeletonHtml;
        const spans = tempRoot.querySelectorAll('span[data-index]');
        spans.forEach(span => {
          const idx = parseInt(span.getAttribute('data-index')!, 10);
          span.textContent = accumulatedTexts[idx] || '';
        });
        const updatedHtml = tempRoot.innerHTML;

        // Update the message in the state
        const updatedMessages = this.stateSignal().messages.map(m =>
          m.id === msgId ? { ...m, text: updatedHtml } : m
        );
        this.updateState({ messages: updatedMessages });
        await this.delay(30);
      }

      // 7. Finally, replace with the original full HTML to clean up placeholders
      const finalMessages = this.stateSignal().messages.map(m =>
        m.id === msgId ? { ...m, text: fullText } : m
      );
      this.updateState({ messages: finalMessages });
    } catch (err) {
      // Fallback: add the message directly
      this.addMessage(fullText, 'assistant');
    } finally {
      this.updateState({ isStreaming: false });
    }
  }

  // Fallback streaming for plain text (no HTML)
  private async streamPlainText(fullText: string): Promise<void> {
    const id = this.generateId();
    const msg: ChatMessage = {
      id,
      text: '',
      sender: 'assistant',
      timestamp: new Date()
    };
    const current = this.stateSignal().messages;
    this.updateState({ messages: [...current, msg], isStreaming: true });

    const words = fullText.split(/\s+/).filter(w => w.length > 0);
    let currentText = '';
    for (let i = 0; i < words.length; i++) {
      if (i > 0) currentText += ' ';
      currentText += words[i];
      const updatedMessages = this.stateSignal().messages.map(m =>
        m.id === id ? { ...m, text: currentText } : m
      );
      this.updateState({ messages: updatedMessages });
      await this.delay(30);
    }
    // The final text is already fullText, so no extra step needed
    this.updateState({ isStreaming: false });
  }

  // ---------- Command and Step Processing (unchanged) ----------
  private async processCommand(input: string): Promise<string> {
    const trimmed = input.trim();

    let matchedCommand = this.commands.find(cmd =>
      cmd.name === trimmed || cmd.aliases.includes(trimmed)
    );

    if (!matchedCommand) {
      matchedCommand = this.commands.find(cmd =>
        cmd.pattern && cmd.pattern.test(trimmed)
      );
    }

    if (matchedCommand) {
      const context = this.buildContext();
      try {
        return await matchedCommand.execute(context, trimmed);
      } catch (err: any) {
        return `❌ Command execution failed: ${err.message || 'unknown error'}`;
      }
    }

    return `❓ I don't understand that command. Type "help" to see available commands.`;
  }

  private async processStep(input: string): Promise<string> {
    const state = this.stateSignal();
    const step = state.currentStep;
    const data = state.selectedData;

    switch (step) {
      case 'listing_user': {
        const users = data.users || [];
        const index = parseInt(input, 10) - 1;
        if (isNaN(index) || index < 0 || index >= users.length) {
          return `❌ Invalid selection. Please enter a number between 1 and ${users.length}.`;
        }
        const user = users[index];
        this.updateState({
          currentStep: 'selected_user',
          selectedData: { ...data, selectedIndex: index }
        });
        const detail = this.formatUserDetail(user);
        return `${detail}\n\nWhat would you like to do with this user?\n- Type \`suspend\`\n- Type \`activate\`\n- Type \`blacklist\`\n- Type \`delete\` (if available)`;
      }

      case 'selected_user': {
        const users = data.users || [];
        const index = data.selectedIndex;
        if (index === undefined || index < 0 || index >= users.length) {
          this.resetStep();
          return '❌ User not found. Let\'s start over.';
        }
        const user = users[index];
        const action = input.toLowerCase().trim();
        const actionMap: Record<string, (id: string) => Promise<any>> = {
          suspend: (id) => firstValueFrom(this.apiService.suspendUser(id)),
          activate: (id) => firstValueFrom(this.apiService.activateUser(id)),
          blacklist: (id) => firstValueFrom(this.apiService.blacklistUser(id))
        };
        if (!actionMap[action]) {
          return `❌ Unknown action. Please type \`suspend\`, \`activate\`, or \`blacklist\`.`;
        }
        try {
          await actionMap[action](user.id);
          this.resetStep();
          return `✅ User ${user.displayName || user.email} has been ${action}ed.`;
        } catch (err: any) {
          this.resetStep();
          return `❌ Failed to ${action} user: ${err.message || 'unknown error'}`;
        }
      }

      case 'listing_disputes': {
        const disputes = data.disputes || [];
        const index = parseInt(input, 10) - 1;
        if (isNaN(index) || index < 0 || index >= disputes.length) {
          return `❌ Invalid selection. Please enter a number between 1 and ${disputes.length}.`;
        }
        const dispute = disputes[index];
        this.updateState({
          currentStep: 'selected_disputes',
          selectedData: { ...data, selectedIndex: index }
        });
        const detail = this.formatDisputeDetail(dispute);
        return `${detail}\n\nHow would you like to resolve this dispute?\n- Type \`refund\` (refund buyer)\n- Type \`release\` (release to seller)\n- Type \`partial\` (partial settlement - will ask for amount)`;
      }

      case 'selected_disputes': {
        const disputes = data.disputes || [];
        const index = data.selectedIndex;
        if (index === undefined || index < 0 || index >= disputes.length) {
          this.resetStep();
          return '❌ Dispute not found. Let\'s start over.';
        }
        const dispute = disputes[index];
        const action = input.toLowerCase().trim();
        try {
          if (action === 'refund') {
            await firstValueFrom(this.apiService.resolveDisputeRefund(dispute.id));
            this.resetStep();
            return `✅ Dispute ${this.truncateId(dispute.id)} resolved: Refunded buyer.`;
          } else if (action === 'release') {
            await firstValueFrom(this.apiService.resolveDisputeRelease(dispute.id));
            this.resetStep();
            return `✅ Dispute ${this.truncateId(dispute.id)} resolved: Released funds to seller.`;
          } else if (action === 'partial') {
            this.resetStep();
            return `⚠️ Partial settlement is not yet implemented in this chat. Please use the UI.`;
          } else {
            return `❌ Unknown action. Please type \`refund\`, \`release\`, or \`partial\`.`;
          }
        } catch (err: any) {
          this.resetStep();
          return `❌ Failed to resolve dispute: ${err.message || 'unknown error'}`;
        }
      }

      default:
        this.resetStep();
        return '❓ I didn\'t understand that. Let\'s start over.';
    }
  }

  private buildContext(): CommandContext {
    const state = this.stateSignal();
    return {
      apiService: this.apiService,
      state: state,
      commands: this.commands,
      updateState: (changes: Partial<ChatState>) => this.updateState(changes),
      addMessage: (text: string, sender: ChatSender) => this.addMessage(text, sender),
      setStep: (step: ChatStep, data?: any) => {
        this.updateState({
          currentStep: step,
          selectedData: data || {}
        });
      }
    };
  }

  private updateState(changes: Partial<ChatState>): void {
    this.stateSignal.update(current => ({ ...current, ...changes }));
  }

  private resetStep(): void {
    this.updateState({ currentStep: 'idle', selectedData: {} });
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
  <div><span class="label">Created:</span> ${new Date(user.createdAt).toLocaleString()}</div>
</div>
    `;
  }

  private formatDisputeDetail(dispute: any): string {
    return `
<div class="detail-card">
  <div><span class="label">ID:</span> ${dispute.id}</div>
  <div><span class="label">Status:</span> ${dispute.status}</div>
  <div><span class="label">Amount:</span> KES ${dispute.amount}</div>
  <div><span class="label">Raised By:</span> ${dispute.raisedBy}</div>
  <div><span class="label">Against:</span> ${dispute.against}</div>
  <div><span class="label">Reason:</span> ${dispute.reason}</div>
  <div><span class="label">Created:</span> ${new Date(dispute.createdAt).toLocaleString()}</div>
</div>
    `;
  }

  private truncateId(id: string, length: number = 8): string {
    return id.length > length ? id.slice(0, length) + '…' : id;
  }
}