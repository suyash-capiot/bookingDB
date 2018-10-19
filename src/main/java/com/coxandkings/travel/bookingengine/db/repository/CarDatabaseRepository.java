package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;

public interface CarDatabaseRepository  extends JpaRepository<CarOrders, Serializable> {
	
	public CarOrders saveOrder(CarOrders orderObj, String prevOrder);

	public List<CarOrders> findByBooking(Booking booking);
	
	public List<CarOrders> findBysuppID(String suppID);

	public List<CarOrders> findBySuppReservationId(String suppId, String id);
	public List<CarOrders> getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef);

}
