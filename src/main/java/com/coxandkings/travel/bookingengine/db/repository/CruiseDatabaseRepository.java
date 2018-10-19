package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;

public interface CruiseDatabaseRepository extends JpaRepository<CruiseOrders, Serializable> {

	public CruiseOrders saveOrder(CruiseOrders orderObj, String prevOrder);

	public List<CruiseOrders> findByBooking(Booking booking);
	
	public List<CruiseOrders> findBysuppID(String suppID);
	
	public CruiseOrders findByReservationID(String reservationID);
	public CruiseOrders findByReservationIDandBookID(String reservationID,String bookID);
	
}
