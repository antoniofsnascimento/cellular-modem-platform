package com.celer.client;

public record CommandResult(
    String command,
    String response,
    long latencyMs,
    boolean success
) {
    public static CommandResult of(String command, String response, long latencyMs) {
        boolean ok = response != null && (response.contains("OK") || response.startsWith("+"));
        return new CommandResult(command, response, latencyMs, ok);
    }

    public static CommandResult failure(String command, String error, long latencyMs) {
        return new CommandResult(command, error, latencyMs, false);
    }
}
