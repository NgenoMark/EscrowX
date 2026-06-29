/* In here we define the chat model and its features */

import { ApiService } from "../../../core/services/api.service";

export type ChatSender = 'user' | 'assistant';

/* Defining a single Chat message */
export interface ChatMessage {
    id?: string;                // unique identifier for streaming updates
    text: string;
    sender: ChatSender;
    timestamp: Date;
    data?: any;
}

/** Checking whether the user is in a multi-step conversation 
    * idle means were waiting for a top level-command
    * Others means were inside a flow
**/

export type ChatStep = 
    | 'idle'
    | 'listing_user'
    | 'selected_user'
    | 'listing_disputes'
    | 'selected_disputes'

/**
 * Defining the entire state of the window amd also conversation that is happening
 * 
 */

export interface ChatState {
    messages: ChatMessage[];
    /** Checks whether a chat window is open or closed */
    isOpen: boolean;
    /** Are we waiting for an API response (We show typing indicator)  */
    isLoading: boolean;
    inputText: string;
    currentStep: ChatStep;
    /** Data needed when in multiflow */
    selectedData: {
        users?: any[];
        disputes?: any[];
        selectedIndex?: number;
        pendingAction?: string;
    }
    isStreaming: boolean;       // true while a message is being streamed
}


/**
 * Defining the commands since they are self-contained
 * Examples: HelpCommand, ReportCommand, ListUsersCommand, Etc
 */

export interface ChatCommand {
    // The primary name of the command
    name: string;
    // alternative names the user can type for the command e.g ?, commands
    aliases: string[];
    // short description for the help menu
    description: string;
    // optional regex patterns for commands that need arguments (e.g user 123)
    pattern?: RegExp;
    // Execute the command and @returns the response text to show to the user
    execute(context: CommandContext, input: string): Promise<string>;
}

/**
 * We define everything that a command needs to interact with the chat
 * This is passed to every command's execute Method
 */

export interface CommandContext {
    apiService: ApiService;
    state: ChatState;
    commands: ChatCommand[];
    updateState: ( changes: Partial<ChatState>) => void;
    addMessage: ( text: string, sender: ChatSender) => void;
    setStep: (step: ChatStep, data?: any) => void;
}