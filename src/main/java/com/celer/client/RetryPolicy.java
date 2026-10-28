package com.celer.client;

import java.util.concurrent.Callable;

public final class RetryPolicy {
    private final int maxRetries;
    private final long initialDelayMs;

    public RetryPolicy(int maxRetries, long initialDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    public CommandResult execute(Callable<CommandResult> operation) {
        int attempts = 0;
        long currentDelay = initialDelayMs;
        CommandResult lastResult = null;

        while (attempts <= maxRetries) {
            attempts++;
            try {
                lastResult = operation.call();
                if (lastResult.success()) {
                    return lastResult;
                }
            } catch (Exception e) {
                lastResult = CommandResult.failure("RETRY_OP", e.getMessage(), 0);
            }

            if (attempts <= maxRetries) {
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return lastResult;
                }
                currentDelay *= 2;
            }
        }
        return lastResult;
    }
}
