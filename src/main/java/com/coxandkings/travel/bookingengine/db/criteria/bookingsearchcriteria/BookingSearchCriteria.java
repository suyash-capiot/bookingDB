package com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingSearchCriteria {

    private int size;
    private int page;
    private String sortingOrder;
    private String sortingField;

    @JsonProperty( "favBookingDetailsResource" )
    private BookingDetailsFilter bookingBasedFilter = new BookingDetailsFilter();

    @JsonProperty( "favProductDetailsResource" )
    private ProductDetailsFilter productBasedFilter = new ProductDetailsFilter();

    @JsonProperty( "favCompanyDetailsResource" )
    private CompanyDetailsFilter companyBasedFilter = new CompanyDetailsFilter();

    @JsonProperty( "favCliAndPassengerDetailsResource" )
    private ClientAndPassengerBasedFilter clientPxBasedFilter = new ClientAndPassengerBasedFilter();

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getSortingOrder() {
        return sortingOrder;
    }

    public void setSortingOrder(String sortingOrder) {
        this.sortingOrder = sortingOrder;
    }

    public String getSortingField() {
        return sortingField;
    }

    public void setSortingField(String sortingField) {
        this.sortingField = sortingField;
    }

    public BookingDetailsFilter getBookingBasedFilter() {
        return bookingBasedFilter;
    }

    public void setBookingBasedFilter(BookingDetailsFilter bookingBasedFilter) {
        this.bookingBasedFilter = bookingBasedFilter;
    }

    public ProductDetailsFilter getProductBasedFilter() {
        return productBasedFilter;
    }

    public void setProductBasedFilter(ProductDetailsFilter productBasedFilter) {
        this.productBasedFilter = productBasedFilter;
    }

    public CompanyDetailsFilter getCompanyBasedFilter() {
        return companyBasedFilter;
    }

    public void setCompanyBasedFilter(CompanyDetailsFilter companyBasedFilter) {
        this.companyBasedFilter = companyBasedFilter;
    }

    public ClientAndPassengerBasedFilter getClientPxBasedFilter() {
        return clientPxBasedFilter;
    }

    public void setClientPxBasedFilter(ClientAndPassengerBasedFilter clientPxBasedFilter) {
        this.clientPxBasedFilter = clientPxBasedFilter;
    }
}
