import { Component, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatbotService } from './services/chatbot.service';
import { ChatMessage, ChatState } from './models/chat.models';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.html',
  styleUrls: ['./chatbot.css']
})
export class ChatbotComponent implements AfterViewChecked {
  private chatbotService = inject(ChatbotService);

  @ViewChild('messageContainer') messageContainer!: ElementRef;

  get state(): ChatState {
    return this.chatbotService.state();
  }

  get messages(): ChatMessage[] {
    return this.state.messages;
  }

  get isOpen(): boolean {
    return this.state.isOpen;
  }

  get isLoading(): boolean {
    return this.state.isLoading;
  }

  // ✅ New: expose streaming state
  get isStreaming(): boolean {
    return this.state.isStreaming;
  }

  get inputText(): string {
    return this.state.inputText;
  }

  set inputText(value: string) {
    this.chatbotService.setInputText(value);
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    if (this.messageContainer) {
      const element = this.messageContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  toggleChat(): void {
    this.chatbotService.toggleChat();
  }

  sendMessage(): void {
    const text = this.inputText;
    if (!text?.trim()) return;
    this.chatbotService.sendMessage(text);
  }

  formatMessage(text: string): string {
    if (!text) return '';
    let safe = text.replace(/<script/g, '&lt;script');
    safe = safe.replace(/<\/script/g, '&lt;/script');
    return safe.replace(/\n/g, '<br>');
  }
}