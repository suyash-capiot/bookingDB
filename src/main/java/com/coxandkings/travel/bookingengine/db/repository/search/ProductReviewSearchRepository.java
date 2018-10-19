package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.resource.productreview.ProductReviewInfo;

import java.sql.SQLException;
import java.util.List;

public interface ProductReviewSearchRepository {
    public List<ProductReviewInfo> searchGeneralArrivalList(String productEndDate) throws SQLException;
}
