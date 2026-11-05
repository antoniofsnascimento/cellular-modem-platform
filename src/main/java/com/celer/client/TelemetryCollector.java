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
            " TELEMETRY SUMMARY \n" +
            "Total Commands Executed: %d\n" +
            "Avg Command Latency:     %.2f ms\n" +
            "Max Command Latency:     %d ms\n" +
            "Command Error Rate:      %.2f%%\n" +
            "Total Votes Received:    %d\n" +
            "Voting Breakdown:        %s\n" +
            "Current Modem Balance:   %.2f EUR\n" +
            "--------------------------------------",
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
            json.append(String.format("      \"comment\": \"%s\",\n", getHardwareComment(r.command())));
            json.append(String.format("      \"latencyMs\": %d,\n", r.latencyMs()));
            json.append(String.format("      \"success\": %b\n", r.success()));
            json.append("    }").append(i == records.size() - 1 ? "\n" : ",\n");
        }
        json.append("  ]\n}");

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        }
    }

    private static String getHardwareComment(String command) {
        if (command == null) return "Generic Hayes AT modem operation";
        String upper = command.trim().toUpperCase();
        if (upper.equals("AT")) {
            return "UART autobauding sync & baseband processor readiness verification";
        } else if (upper.equals("ATI")) {
            return "Query baseband firmware version and modem hardware identity";
        } else if (upper.startsWith("AT+CPIN?")) {
            return "Poll ISO 7816 SIM card physical presence and PIN lock status";
        } else if (upper.startsWith("AT+CPIN=")) {
            return "Authenticate SIM PIN, release IMSI and power up RF transceiver";
        } else if (upper.startsWith("AT+CREG?")) {
            return "Poll network registration state (BCCH cell scanning vs attached)";
        } else if (upper.startsWith("AT+COPS?")) {
            return "Query serving cell operator PLMN name and Radio Access Technology";
        } else if (upper.startsWith("AT+CSQ")) {
            return "Measure antenna Received Signal Strength (RSSI) in dBm and BER";
        } else if (upper.startsWith("AT+CMGF=")) {
            return "Configure SMS framing protocol mode (Text mode vs raw PDU)";
        } else if (upper.startsWith("AT+CSCA?")) {
            return "Query SMSC gateway routing address stored in SIM EF_SMSP file";
        } else if (upper.startsWith("AT+CSCA=")) {
            return "Update SMSC gateway routing address in SIM card memory";
        } else if (upper.startsWith("AT+CUSD=")) {
            return "Open high-priority USSD signaling session with core network HLR/HSS";
        } else if (upper.startsWith("AT+INJECT=")) {
            return "Simulate downlink radio burst arriving from cell tower into SIM memory";
        } else if (upper.startsWith("AT+CMGL")) {
            return "List message slots allocated in SIM EEPROM storage memory (SM)";
        } else if (upper.startsWith("AT+CMGR=")) {
            return "Read message payload from SIM slot and toggle status to REC READ";
        } else if (upper.startsWith("AT+CMGD=")) {
            return "Erase message from SIM EEPROM to prevent capacity exhaustion (+CMS ERROR: 322)";
        } else if (upper.startsWith("AT+CMGS")) {
            return "Transmit SMS frame via uplink dedicated signaling channel to SMSC";
        }
        return "Standard 3GPP cellular modem command execution";
    }
}
