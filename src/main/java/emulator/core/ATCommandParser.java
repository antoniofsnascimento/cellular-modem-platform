package emulator.core;

public class ATCommandParser {
    private boolean echoEnabled = true; // ATE0/ATE1
    private boolean extendedErrors = false; // AT+CMEE = 1

    private int networkState = 1; // network states

    private String processCommand(String rawCommand){
        String command = rawCommand.trim().toUpperCase(); // empty spaces and case-sensitive

        if (command.isEmpty()){
            return "";
        }

        String response = evaluateCommand(command);

       return response + "\r\n";

    }

    private String evaluateCommand(String command){
        return switch (command) {
            case "AT" -> "OK";
            case "ATE0" -> {echoEnabled = false; yield "OK";}
            case "ATE1" -> {echoEnabled = true; yield "OK";}
            case "ATI" -> "Cellular Emulator v1.0\r\nOK";
            case "AT+CMEE=1" -> {extendedErrors = true; yield "OK";}
            case "AT+CPIN?" -> "+CPIN: READY\r\nOK";
            case "AT+COPS?" -> "+COPS: 0,0,\"UTAD Mobile\"\r\nOK";
            case "AT+CREG?", "AT+CGREG?" -> "+CREG: 0," + networkState + "\r\nOK";
            case "AT+CSQ" -> "+CSQ: 20,99\r\nOK";
            default -> extendedErrors ? "+CME ERROR: 3" : "ERROR";
        };
    }

    public boolean isEchoEnabled(){
        return echoEnabled;
    }

}