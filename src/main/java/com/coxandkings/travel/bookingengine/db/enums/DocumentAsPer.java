package com.coxandkings.travel.bookingengine.db.enums;

public enum DocumentAsPer {
    BOOKING("BOOKING"),
    PAX("PAX"),
    ROOM("ROOM");

    private String documentAsPer;

    DocumentAsPer(String documentAsPer) {
        this.documentAsPer = documentAsPer;

    }

    public String getDocumentAsPer() {
        return documentAsPer;
    }

    public static DocumentAsPer fromString(String newStatus) {
        DocumentAsPer documentAsPer = null;
        if (newStatus != null && !newStatus.isEmpty()) {
            for (DocumentAsPer tmpBookingStatus : DocumentAsPer.values()) {
                if (tmpBookingStatus.getDocumentAsPer().equalsIgnoreCase(newStatus)) {
                    documentAsPer = tmpBookingStatus;
                    break;
                }
            }
        }
        return documentAsPer;
    }
}
