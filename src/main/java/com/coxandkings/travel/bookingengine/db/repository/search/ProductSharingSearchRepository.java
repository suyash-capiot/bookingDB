package com.coxandkings.travel.bookingengine.db.repository.search;

import com.coxandkings.travel.bookingengine.db.criteria.productsharing.ProductSharingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.productsharing.ProductSharingInfo;

import java.sql.SQLException;
import java.util.List;

public interface ProductSharingSearchRepository {
    public List<ProductSharingInfo> searchProductSharingBookings(ProductSharingSearchCriteria productSharingSearchCriteria) throws SQLException;
}


