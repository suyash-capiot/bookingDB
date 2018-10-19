package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;



public interface AirDatabaseRepository  extends JpaRepository<AirOrders, Serializable> {
	
	public AirOrders saveOrder(AirOrders orderObj, String prevOrder);

	public List<AirOrders> findByBooking(Booking booking);
	
	public List<AirOrders> findBysuppID(String suppID);

	public List<AirOrders> getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef);

	public List<AirOrders> getAirGDSDetailsForGDSPNR(String gdsPNR);
}
