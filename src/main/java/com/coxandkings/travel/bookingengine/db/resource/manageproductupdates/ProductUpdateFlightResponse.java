package com.coxandkings.travel.bookingengine.db.resource.manageproductupdates;

import java.util.List;

public class ProductUpdateFlightResponse {
    private Integer numberOfPages;
    private List<ProductUpdateFlightInfo> productUpdateFlightInfo;

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public List<ProductUpdateFlightInfo> getProductUpdateFlightInfo() {
        return productUpdateFlightInfo;
    }

    public void setProductUpdateFlightInfo(List<ProductUpdateFlightInfo> productUpdateFlightInfo) {
        this.productUpdateFlightInfo = productUpdateFlightInfo;
    }
}
