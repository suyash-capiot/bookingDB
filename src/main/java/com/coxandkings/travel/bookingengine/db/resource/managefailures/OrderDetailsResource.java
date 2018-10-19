package com.coxandkings.travel.bookingengine.db.resource.managefailures;

import java.util.List;

public class OrderDetailsResource {

    private String orderId;

    private List<OrderSummary> orderSummary;

    private String orderStatus;



    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderSummary> getOrderSummary() {
        return orderSummary;
    }

    public void setOrderSummary(List<OrderSummary> orderSummary) {
        this.orderSummary = orderSummary;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

}
