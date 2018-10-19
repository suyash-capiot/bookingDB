package com.coxandkings.travel.bookingengine.db.enums;


public enum BookingStatus {
    CNF( "Confirmed" ),
    RQ( "On Request" ),
    XL("Cancelled"),
    VCH( "Vouchered" );

    private String bookingStatus;

    BookingStatus(String newStatus )    {
        bookingStatus = newStatus;
    }

    public String getBookingStatus()    {
        return bookingStatus;
    }

    public static BookingStatus fromString(String newStatus ) {
    	BookingStatus aBookingStatus = null;
        if (newStatus != null && !newStatus.isEmpty()) {
//            return aBookingStatus;
//       }

            for (BookingStatus tmpBookingStatus : BookingStatus.values()) {
                if (tmpBookingStatus.getBookingStatus().equalsIgnoreCase(newStatus)) {
                    aBookingStatus = tmpBookingStatus;
                    break;
                }
            }
        }
        return aBookingStatus;
    }
}
