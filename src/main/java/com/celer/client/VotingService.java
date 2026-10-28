package com.celer.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VotingService {
    private final Map<String, Integer> votes = new HashMap<>();
    private final Set<String> registeredVoters = new HashSet<>();

    public VotingService(String... options) {
        for (String option : options) {
            votes.put(option.toUpperCase(), 0);
        }
    }

    public synchronized String processVote(String senderNumber, String text) {
        if (registeredVoters.contains(senderNumber)) {
            return "Vote rejected: Number already voted.";
        }
        String choice = text.trim().toUpperCase();
        if (!votes.containsKey(choice)) {
            return "Vote rejected: Invalid option. Available: " + votes.keySet();
        }
        registeredVoters.add(senderNumber);
        votes.put(choice, votes.get(choice) + 1);
        return "Vote accepted for " + choice + ". Thank you!";
    }

    public synchronized Map<String, Integer> getResults() {
        return Collections.unmodifiableMap(new HashMap<>(votes));
    }

    public synchronized int getTotalVotes() {
        return registeredVoters.size();
    }
}
