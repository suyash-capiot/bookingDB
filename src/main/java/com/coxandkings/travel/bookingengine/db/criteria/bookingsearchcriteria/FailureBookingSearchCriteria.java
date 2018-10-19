package com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria;

import com.coxandkings.travel.bookingengine.db.orchestrator.SearchBookingsServiceImpl;

public class FailureBookingSearchCriteria extends  BookingSearchCriteria{

    private String failureFlag;

    public String getFailureFlag() {
        return failureFlag;
    }

    public void setFailureFlag(String failureFlag) {
        this.failureFlag = failureFlag;
    }
}
