package com.coxandkings.travel.bookingengine.db.resource.managecheaperprices;

import java.util.List;

public class CheaperPriceUpdatesFlightInfo extends CheaperPriceBookingInfo   {

    private List<CheaperPriceUpdateFlightSegmentInfo> flightSegments;

    private String tripType;

    private String status;

    public CheaperPriceUpdatesFlightInfo()   {};

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CheaperPriceUpdateFlightSegmentInfo> getFlightSegments() {
        return flightSegments;
    }

    public void setFlightSegments(List<CheaperPriceUpdateFlightSegmentInfo> flightSegments) {
        this.flightSegments = flightSegments;
    }
}

