package com.coxandkings.travel.bookingengine.db.resource.managecheaperprices;

public class CheaperPriceUpdateFlightSegmentInfo {

    private String originLocation;
    private String destinationLocation;
    private String departureDate;
    private String cabintype;

    public CheaperPriceUpdateFlightSegmentInfo()    {
    }

    public String getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(String originLocation) {
        this.originLocation = originLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getCabintype() {
        return cabintype;
    }

    public void setCabintype(String cabintype) {
        this.cabintype = cabintype;
    }
}
