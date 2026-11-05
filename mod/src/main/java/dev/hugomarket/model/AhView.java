package dev.hugomarket.model;

public enum AhView {
    ALL("Alle Auktionen anzeigen"),
    OWN_ACTIVE("Laufende Auktionen"),
    OWN_SOLD("Verkaufte Auktionen"),
    OWN_BOUGHT("Gekaufte Auktionen"),
    OWN_EXPIRED("Abgelaufene Auktionen");

    private final String label;

    AhView(String label) {
        this.label = label;
    }

    public static AhView fromLabel(String label) {
        for (AhView view : values()) {
            if (view.label.equals(label)) {
                return view;
            }
        }
        return ALL;
    }

    public boolean isMarket() {
        return this == ALL;
    }
}
