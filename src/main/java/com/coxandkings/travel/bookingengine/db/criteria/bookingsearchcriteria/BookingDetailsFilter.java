package com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria;

public class BookingDetailsFilter {

    private String bookingRefId; // bookId - DONE

    private String bookingTypeId; // not provided by booking - DONE

    private String bookingFromDate; // from created at date - DONE

    private String bookingToDate; // till created at created date - DONE

    private String bookingStatusId; // status - DONE

    private String priority; // TODO : get it from ops DB priority

    private Boolean assignment; // TODO : get it from ops DB priority

    private String userId; // userID - DONE

    private String financialControlId; // TODO : read the BR and find out

    public String getBookingRefId() {
        return bookingRefId;
    }

    public void setBookingRefId(String bookingRefId) {
        this.bookingRefId = bookingRefId;
    }

    public String getBookingTypeId() {
        return bookingTypeId;
    }

    public void setBookingTypeId(String bookingTypeId) {
        this.bookingTypeId = bookingTypeId;
    }

    public String getBookingFromDate() {
        return bookingFromDate;
    }

    public void setBookingFromDate( String bookingFromDate) {
        this.bookingFromDate = bookingFromDate;
    }

    public String getBookingToDate() {
        return bookingToDate;
    }

    public void setBookingToDate(String bookingToDate) {
        this.bookingToDate = bookingToDate;
    }

    public String getBookingStatusId() {
        return bookingStatusId;
    }

    public void setBookingStatusId(String bookingStatusId) {
        this.bookingStatusId = bookingStatusId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getAssignment() {
        return assignment;
    }

    public void setAssignment(Boolean assignment) {
        this.assignment = assignment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFinancialControlId() {
        return financialControlId;
    }

    public void setFinancialControlId(String financialControlId) {
        this.financialControlId = financialControlId;
    }
}
