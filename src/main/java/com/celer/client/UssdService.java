package com.celer.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UssdService {
    private static final Pattern BALANCE_PATTERN = Pattern.compile("Saldo:\\s*([0-9]+[.,][0-9]{2})\\s*EUR");
    private final double lowBalanceThreshold;

    public UssdService(double lowBalanceThreshold) {
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    public double parseBalance(String rawResponse) {
        Matcher matcher = BALANCE_PATTERN.matcher(rawResponse);
        if (matcher.find()) {
            String valueStr = matcher.group(1).replace(',', '.');
            return Double.parseDouble(valueStr);
        }
        return -1.0;
    }

    public boolean isBalanceCritical(double balance) {
        return balance >= 0.0 && balance < lowBalanceThreshold;
    }
}
