package com.shobuj.store.shared;

import java.util.Optional;

/** Bangladeshi mobile numbers, stored as 01XXXXXXXXX. Accepts +880, 880, spaces and dashes. */
public final class BangladeshPhone {

    private BangladeshPhone() {
    }

    public static Optional<String> normalise(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String digits = input.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+880")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("880")) {
            digits = "0" + digits.substring(3);
        }
        return digits.matches("01[3-9]\\d{8}") ? Optional.of(digits) : Optional.empty();
    }
}
