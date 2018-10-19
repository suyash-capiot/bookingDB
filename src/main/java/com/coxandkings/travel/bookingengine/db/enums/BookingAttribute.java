package com.coxandkings.travel.bookingengine.db.enums;

public enum BookingAttribute {
	


		BF( "Failure Booking" ),
		RAMD( "Request for Amendment" ),
	    AF( "Amendment Failure" ),
	    CF( "Cancellation Failure" ),
	    AMENDED( "Amended" ),
	    RECONFIRMATION_CLIENT_RECONFIRMED( "Client Reconfirmed" ),
	    RECONFIRMATION_SUPPLIER_RECONFIRMED( "Supplier Reconfirmed" ),
	    RECONFIRMATION_PENDING_SUPPLIER_RECONFIRMATION( "Pending Supplier Reconfirmation" ),
	    RECONFIRMATION_PENDING_CLIENT_RECONFIRMATION( "Pending Client Reconfirmation" ),
	    RECONFIRMATION_REJECTED_BY_CLIENT( "Reconfirmation Rejected by Client" ),
	    RECONFIRMATION_REJECTED_BY_SUPPLIER( "Reconfirmation Rejected by Supplier" ),
	    RECONFIRMATION_ON_HOLD_BY_SUPPLIER( "Reconfirmation On Hold by Supplier" ),
	    RECONFIRMATION_ON_HOLD_BY_CUSTOMER( "Reconfirmation On Hold by Customer" ),
	    BOOKING_TYPE_TIME_LIMIT( "Time Limit" ),
	    WORK_IN_PROGRESS( "Work In Progress" );


	    private String bookingAttribute;

	    private BookingAttribute(String newAttribute )    {
	        bookingAttribute = newAttribute;
	    }

	    public String getBookingAttribute()    {
	        return bookingAttribute;
	    }

	    public BookingAttribute fromString(String newAttribute )  {
	    	BookingAttribute aBookingAttribute = null;
	        if( newAttribute == null || newAttribute.isEmpty() )  {
	            return aBookingAttribute;
	        }

	        for( BookingAttribute tmpBookingAttribute : BookingAttribute.values() )    {
	            if( tmpBookingAttribute.getBookingAttribute().equalsIgnoreCase( newAttribute ))  {
	                aBookingAttribute = tmpBookingAttribute;
	                break;
	            }
	        }
	        return aBookingAttribute;
	    }
	}

