package com.celer.client;

public final class ClientApp {
    public static void main(String[] args) {
        String portPath = args.length > 0 ? args[0] : "/tmp/ttyB";
        TelemetryCollector telemetry = new TelemetryCollector();
        VotingService votingService = new VotingService("OPCAO_A", "OPCAO_B");
        UssdService ussdService = new UssdService(2.00);
        RetryPolicy retryPolicy = new RetryPolicy(3, 500);

        System.out.println("Starting Cellular Client on: " + portPath);

        try (SerialModemDriver driver = new SerialModemDriver(portPath)) {
            driver.open();

            executeAndRecord(driver, telemetry, "AT");
            executeAndRecord(driver, telemetry, "AT+CPIN=0000");

            System.out.println("Waiting for network registration...");
            Thread.sleep(5500);
            executeAndRecord(driver, telemetry, "AT+CREG?");
            executeAndRecord(driver, telemetry, "AT+CSQ");

            CommandResult ussdResult = executeAndRecord(driver, telemetry, "AT+CUSD=1,\"*#100#\",15");
            double balance = ussdService.parseBalance(ussdResult.response());
            System.out.println("Current Balance: " + balance + " EUR");
            if (ussdService.isBalanceCritical(balance)) {
                System.out.println("WARNING: Low balance alert triggered!");
            }

            System.out.println("\n--- Processing Simulated Incoming Votes ---");
            String voter1 = "+351912345678";
            String vote1Response = votingService.processVote(voter1, "OPCAO_A");
            System.out.println("Incoming Vote from " + voter1 + ": " + vote1Response);

            System.out.println("Sending confirmation SMS with retry policy...");
            CommandResult smsResult = retryPolicy.execute(() -> {
                CommandResult res = driver.sendSms(voter1, vote1Response);
                telemetry.record(res);
                return res;
            });
            System.out.println("SMS Sent: " + smsResult.success() + " (" + smsResult.latencyMs() + " ms)");

            String vote2Response = votingService.processVote(voter1, "OPCAO_B");
            System.out.println("Duplicate Vote from " + voter1 + ": " + vote2Response);

            System.out.println("\n" + telemetry.generateSummary(votingService, balance));
            telemetry.exportToJson("telemetry.json", votingService, balance);
            System.out.println("Telemetry written to telemetry.json");

        } catch (Exception e) {
            System.err.println("Client encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static CommandResult executeAndRecord(SerialModemDriver driver, TelemetryCollector telemetry, String command) {
        CommandResult result = driver.execute(command);
        telemetry.record(result);
        System.out.println("[CMD] " + command + " -> " + result.response().replace("\r\n", " ") + " (" + result.latencyMs() + "ms)");
        return result;
    }
}
