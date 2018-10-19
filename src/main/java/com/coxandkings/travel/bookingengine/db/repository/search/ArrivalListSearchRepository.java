package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.FlightArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.GeneralArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.HotelArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListGeneralInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListHotelInfo;

import java.sql.SQLException;
import java.util.List;

public interface ArrivalListSearchRepository {
    public List<ArrivalListHotelInfo> searchHotelArrivalList(HotelArrivalListSearchCriteria arrivalListSearchCriteria)
            throws SQLException, BookingEngineDBException;

    public List<ArrivalListFlightInfo> searchFlightArrivalList(FlightArrivalListSearchCriteria
                                                                      flightArrivalListSearchCriteria) throws SQLException;


    public List<ArrivalListGeneralInfo> searchGeneralArrivalList(GeneralArrivalListSearchCriteria
                                                                       generalArrivalListSearchCriteria) throws SQLException;
}
