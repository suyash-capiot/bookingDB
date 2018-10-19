package com.coxandkings.travel.bookingengine.db.resource.managefailures;

import java.util.List;


public class ProductSummary {

    private String productSubCategory;

    private List<OrderDetailsResource> orders;

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public List<OrderDetailsResource> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDetailsResource> orders) {
        this.orders = orders;
    }
}
