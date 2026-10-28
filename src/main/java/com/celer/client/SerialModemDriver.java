package com.celer.client;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class SerialModemDriver implements AutoCloseable {
    private final String portPath;
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SerialModemDriver(String portPath) {
        this.portPath = portPath;
    }

    public void open() {
        serialPort = SerialPort.getCommPort(portPath);
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 3000, 0);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Failed to open virtual port: " + portPath);
        }
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
    }

    public CommandResult execute(String command) {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new IllegalStateException("Port not open");
        }
        long startTime = System.currentTimeMillis();
        try {
            outputStream.write((command + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            String response = readUntilTerminator();
            long latency = System.currentTimeMillis() - startTime;
            return CommandResult.of(command, response, latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return CommandResult.failure(command, e.getMessage(), latency);
        }
    }

    public CommandResult sendSms(String number, String message) {
        long startTime = System.currentTimeMillis();
        try {
            outputStream.write(("AT+CMGS=\"" + number + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            waitForPrompt();

            outputStream.write((message + "\u001A").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            String response = readUntilTerminator();
            long latency = System.currentTimeMillis() - startTime;
            return CommandResult.of("AT+CMGS=" + number, response, latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return CommandResult.failure("AT+CMGS=" + number, e.getMessage(), latency);
        }
    }

    private void waitForPrompt() throws Exception {
        int character;
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (inputStream.available() > 0) {
                character = inputStream.read();
                if (character == '>') return;
            }
            Thread.sleep(10);
        }
        throw new IllegalStateException("Timeout waiting for '>' prompt");
    }

    private String readUntilTerminator() throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        byte[] buffer = new byte[1024];
        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {
            if (inputStream.available() > 0) {
                int readBytes = inputStream.read(buffer);
                if (readBytes > 0) {
                    responseBuilder.append(new String(buffer, 0, readBytes, StandardCharsets.UTF_8));
                    String current = responseBuilder.toString();
                    if (current.contains("OK\r\n") || current.contains("ERROR\r\n")) {
                        break;
                    }
                }
            } else {
                Thread.sleep(20);
            }
        }
        return responseBuilder.toString().trim();
    }

    @Override
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
