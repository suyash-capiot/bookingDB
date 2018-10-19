package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.criteria.manageproductupdates.FlightUpdatesSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceBookingInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightResponse;

import java.sql.SQLException;
import java.util.List;

public interface ProductUpdatesSearchRepository {
    public List<? extends CheaperPriceBookingInfo> searchCheaperPriceBookings(String productSubCategory) throws SQLException;
    public ProductUpdateFlightResponse searchFlightsForProductUpdates(FlightUpdatesSearchCriteria searchCriteria)throws SQLException;
}
