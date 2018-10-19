package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.FailureBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.managefailures.FailedBookingsSearchResponse;
import com.coxandkings.travel.bookingengine.db.resource.managefailures.FailureDetailsResource;

import java.sql.SQLException;
import java.util.List;

public interface ManageFailuresRepository {
    String searchDuplicateBookings(List<String> searchCriteria, String bookId)
            throws SQLException;

    public List<FailedBookingsSearchResponse> searchFailedBookings(FailureBookingSearchCriteria aBookingSearchCriteria);
}
