package com.shobuj.store.catalog;

import java.util.Arrays;
import java.util.Optional;

/** The care facts shoppers filter by. Labels are what the web app shows. */
public final class Care {

    private Care() {
    }

    public enum Light {
        LOW("Low light"), MEDIUM("Medium light"), BRIGHT("Bright indirect"), FULL_SUN("Full sun");

        public final String label;

        Light(String label) {
            this.label = label;
        }
    }

    public enum Level {
        EASY("Easy"), MODERATE("Moderate"), EXPERT("Needs experience");

        public final String label;

        Level(String label) {
            this.label = label;
        }
    }

    public enum Placement {
        INDOOR("Indoor"), OUTDOOR("Outdoor"), BOTH("Indoor or outdoor");

        public final String label;

        Placement(String label) {
            this.label = label;
        }
    }

    public enum Size {
        SMALL("Small"), MEDIUM("Medium"), LARGE("Large");

        public final String label;

        Size(String label) {
            this.label = label;
        }
    }

    static <E extends Enum<E>> Optional<E> parse(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(value.trim().replace('-', '_')))
                .findFirst();
    }
}
