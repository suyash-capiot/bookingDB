package com.coxandkings.travel.bookingengine.db.enums;

public enum OrderStatus {
    OK("Confirmed"),
    RQ("On Request"),
    TKD("Ticketed"),
    VCH("Vouchered"),
    WL("Waitlisted"),
    RXL("Request for Cancellation"),
    XL("Cancelled"),
    REJ("Rejected");

    private String productStatus;

    private OrderStatus(String newStatus )    {
        productStatus = newStatus;
    }

    public String getProductStatus()    {
        return productStatus;
    }

    public static  OrderStatus fromString(String newStatus )  {
        OrderStatus aProductStatus = null;
        if( newStatus == null || newStatus.isEmpty() )  {
            return aProductStatus;
        }

        for( OrderStatus tmpProductStatus : OrderStatus.values() )    {
            if( tmpProductStatus.getProductStatus().equalsIgnoreCase( newStatus ))  {
                aProductStatus = tmpProductStatus;
                break;
            }
        }
        return aProductStatus;
    }
}