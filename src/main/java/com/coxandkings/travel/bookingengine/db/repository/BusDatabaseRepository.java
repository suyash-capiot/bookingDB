package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

public interface BusDatabaseRepository extends JpaRepository<BusOrders, Serializable>{

	public BusOrders saveOrder(BusOrders orderObj, String prevOrder);

	public List<BusOrders> findByBooking(Booking booking);
	
	//TODO: is it right way by find order by tkt no
	public List<BusAmCl> findByEntityId(String entityId);

	public List<BusOrders> findByTktNo(String string);

	public List<BusOrders> getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef);

	
}
