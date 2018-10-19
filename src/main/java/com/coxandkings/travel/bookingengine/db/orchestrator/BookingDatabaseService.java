package com.coxandkings.travel.bookingengine.db.orchestrator;

import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;

public interface BookingDatabaseService {
	
	public Booking processBookRequest(JSONObject reqJson, boolean isholiday) throws BookingEngineDBException;
	public String processBookResponse(JSONObject resJson) throws BookingEngineDBException;
	public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException;
	
}
