package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.criteria.mergebooking.MergeBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.mergebooking.MergeBookingInfo;

import java.sql.SQLException;
import java.util.List;

public interface MergeBookingSearchRepository {
    public String searchMergeBookings(MergeBookingSearchCriteria
                                                              mergeBookingSearchCriteria) throws SQLException;
    public String getMergeBookings() throws SQLException;
}
