/**
 * This contains base class for all chat commands
 * provides common utilities: API call wrapers, error formatting etc.
 */

import { ChatCommand, CommandContext } from "../models/chat.models"
import { firstValueFrom } from "rxjs"
import { Observable } from "rxjs"

// Abstract base for all commands
export abstract class BaseCommand implements ChatCommand {
    abstract name: string;
    abstract aliases: string[];
    abstract description: string;
    abstract pattern?: RegExp;

    abstract execute(context: CommandContext, input: string): Promise<string>;

    /**
     * Safely call an API observable and handle errors
     */

    protected async callApi<T>(
        apiCall: () => Observable<T>,
        fallbackMessage?: string
    ): Promise<T> {
        try {
            const result = await firstValueFrom(apiCall());
            return result;
        } catch (error: any) {
            throw new Error(
                error?.message || error?.error?.message || 'Unknown API error'
            );
        }
    }


    /* Wrapper to execute an API call and return either the data or an error message
        This is useful for commands that dont want to handle try/catch themselves
    */
    protected async executeApiCall<T>(
        context: CommandContext,
        apiCall: () => Observable<T>,
        onSuccess: (data: T) => string,
        onError?: string
    ): Promise<string> {
        try {
            const data = await this.callApi(apiCall);
            return onSuccess(data);
        } catch (err: any) {
            const message = onError || `❌ ${err.message || 'Operation failed.'}`;
            return message;
        }
    }
    protected formatDate(date: Date | string): string {
        const d = typeof date === 'string' ? new Date(date) : date;
        return d.toLocaleString();
    }

    protected truncateId(id: string, length: number = 8): string {
        return id.length > length ? id.slice(0, length) + '…' : id;
    }
}
