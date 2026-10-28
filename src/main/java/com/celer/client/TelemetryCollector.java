package com.celer.client;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TelemetryCollector {
    private final List<CommandResult> records = Collections.synchronizedList(new ArrayList<>());
    private final Instant sessionStart = Instant.now();

    public void record(CommandResult result) {
        records.add(result);
    }

    public double getAverageLatencyMs() {
        return records.stream().mapToLong(CommandResult::latencyMs).average().orElse(0.0);
    }

    public long getMaxLatencyMs() {
        return records.stream().mapToLong(CommandResult::latencyMs).max().orElse(0L);
    }

    public double getErrorRate() {
        if (records.isEmpty()) return 0.0;
        long failures = records.stream().filter(r -> !r.success()).count();
        return (double) failures / records.size();
    }

    public String generateSummary(VotingService votingService, double lastBalance) {
        return String.format(
            "=== TELEMETRY SUMMARY ===\n" +
            "Total Commands Executed: %d\n" +
            "Avg Command Latency:     %.2f ms\n" +
            "Max Command Latency:     %d ms\n" +
            "Command Error Rate:      %.2f%%\n" +
            "Total Votes Received:    %d\n" +
            "Voting Breakdown:        %s\n" +
            "Current Modem Balance:   %.2f EUR\n" +
            "=========================",
            records.size(),
            getAverageLatencyMs(),
            getMaxLatencyMs(),
            getErrorRate() * 100.0,
            votingService.getTotalVotes(),
            votingService.getResults(),
            lastBalance
        );
    }

    public void exportToJson(String filePath, VotingService votingService, double balance) throws IOException {
        StringBuilder json = new StringBuilder("{\n");
        json.append(String.format("  \"sessionStart\": \"%s\",\n", sessionStart));
        json.append(String.format("  \"totalCommands\": %d,\n", records.size()));
        json.append(String.format("  \"avgLatencyMs\": %.2f,\n", getAverageLatencyMs()));
        json.append(String.format("  \"maxLatencyMs\": %d,\n", getMaxLatencyMs()));
        json.append(String.format("  \"errorRate\": %.4f,\n", getErrorRate()));
        json.append(String.format("  \"currentBalance\": %.2f,\n", balance));
        json.append("  \"votingResults\": {\n");
        var votes = votingService.getResults();
        int idx = 0;
        for (var entry : votes.entrySet()) {
            json.append(String.format("    \"%s\": %d%s\n", entry.getKey(), entry.getValue(), (++idx == votes.size() ? "" : ",")));
        }
        json.append("  },\n");
        json.append("  \"log\": [\n");
        for (int i = 0; i < records.size(); i++) {
            CommandResult r = records.get(i);
            json.append("    {\n");
            json.append(String.format("      \"cmd\": \"%s\",\n", r.command().replace("\"", "\\\"")));
            json.append(String.format("      \"latencyMs\": %d,\n", r.latencyMs()));
            json.append(String.format("      \"success\": %b\n", r.success()));
            json.append("    }").append(i == records.size() - 1 ? "\n" : ",\n");
        }
        json.append("  ]\n}");

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        }
    }
}
