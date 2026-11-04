package com.celer.client;

public final class ClientApp {
    public static void main(String[] args) {
        String portPath = args.length > 0 ? args[0] : "/tmp/ttyB";
        TelemetryCollector telemetry = new TelemetryCollector();
        VotingService votingService = new VotingService("OPCAO_A", "OPCAO_B");
        UssdService ussdService = new UssdService(2.00);
        RetryPolicy retryPolicy = new RetryPolicy(3, 500);

        System.out.println("=================================================");
        System.out.println("  Cellular Modem Platform - Automated Test Suite ");
        System.out.println("  Connected Serial Port: " + portPath);
        System.out.println("=================================================\n");

        try (SerialModemDriver driver = new SerialModemDriver(portPath)) {
            driver.open();

            // --- 1. Basic Diagnostics ---
            System.out.println(">>> 1. Basic Hayes Diagnostics");
            executeAndRecord(driver, telemetry, "AT");
            executeAndRecord(driver, telemetry, "ATI");

            // --- 2. Security & SIM Initialization ---
            System.out.println("\n>>> 2. Security & SIM Status");
            CommandResult cpinCheck = executeAndRecord(driver, telemetry, "AT+CPIN?");
            if (cpinCheck.response().contains("SIM PIN")) {
                executeAndRecord(driver, telemetry, "AT+CPIN=0000");
            }

            // --- 3. Network Lifecycle: Scanning (0,2) -> Registered (0,1) ---
            System.out.println("\n>>> 3. Network Registration Lifecycle");
            executeAndRecord(driver, telemetry, "AT+CREG?");

            System.out.println("Waiting for network attachment...");
            for (int i = 0; i < 6; i++) {
                Thread.sleep(1000);
                CommandResult regPoll = executeAndRecord(driver, telemetry, "AT+CREG?");
                if (regPoll.response().contains(",1")) {
                    System.out.println("SUCCESS: Network attached (Home Network)!");
                    break;
                }
            }
            executeAndRecord(driver, telemetry, "AT+COPS?");
            executeAndRecord(driver, telemetry, "AT+CSQ");

            // --- 4. SMS Configuration ---
            System.out.println("\n>>> 4. SMS Configuration");
            executeAndRecord(driver, telemetry, "AT+CMGF=1");
            executeAndRecord(driver, telemetry, "AT+CSCA?");

            // --- 5. High-Level Feature 1: USSD Balance Inquiry ---
            System.out.println("\n>>> 5. High-Level Feature: USSD Balance Inquiry");
            CommandResult ussdResult = executeAndRecord(driver, telemetry, "AT+CUSD=1,\"*#100#\",15");
            double balance = ussdService.parseBalance(ussdResult.response());
            System.out.println("Current Balance: " + balance + " EUR");
            if (ussdService.isBalanceCritical(balance)) {
                System.out.println("WARNING: Low balance alert triggered!");
            }

            // --- 6. High-Level Feature 2: SMS Voting Center & Storage Management ---
            System.out.println("\n>>> 6. High-Level Feature: SMS Voting Center");
            String voter = "+351912345678";
            executeAndRecord(driver, telemetry, "AT+INJECT=\"" + voter + "\",\"OPCAO_A\"");
            executeAndRecord(driver, telemetry, "AT+CMGL=\"ALL\"");
            executeAndRecord(driver, telemetry, "AT+CMGR=1");

            String voteResponse = votingService.processVote(voter, "OPCAO_A");
            System.out.println("Vote Decision for " + voter + ": " + voteResponse);

            executeAndRecord(driver, telemetry, "AT+CMGD=1");

            String duplicateResponse = votingService.processVote(voter, "OPCAO_B");
            System.out.println("Duplicate Vote Decision: " + duplicateResponse);

            // --- 7. Extension: Resilient SMS Confirmation (Retry Policy) ---
            System.out.println("\n>>> 7. Extension: Resilient SMS Transmission");
            CommandResult smsResult = retryPolicy.execute(() -> {
                CommandResult res = driver.sendSms(voter, voteResponse);
                telemetry.record(res);
                return res;
            });
            System.out.println("Confirmation SMS Sent: " + smsResult.success() + " (" + smsResult.latencyMs() + "ms)");

            // --- 8. Telemetry & Metric Reporting (Week 7) ---
            System.out.println("\n" + telemetry.generateSummary(votingService, balance));
            telemetry.exportToJson("telemetry.json", votingService, balance);
            System.out.println("Telemetry written to telemetry.json\n");

        } catch (Exception e) {
            System.err.println("Client encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static CommandResult executeAndRecord(SerialModemDriver driver, TelemetryCollector telemetry, String command) {
        CommandResult result = driver.execute(command);
        telemetry.record(result);
        System.out.println("[CMD] " + command + " -> " + result.response().replace("\r\n", " ").trim() + " (" + result.latencyMs() + "ms)");
        return result;
    }
}
