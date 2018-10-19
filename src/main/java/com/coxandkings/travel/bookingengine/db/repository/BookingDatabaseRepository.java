package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.searchviewfilter.BookingSearchResponseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.Booking;

public interface BookingDatabaseRepository extends JpaRepository<Booking, Serializable> {

	public Booking saveOrder(Booking orderObj, String prevBooking);
	
	public List<Booking> findByUserID(String userID);

	public List<Booking> findByStatus(String status);

	public List<Booking> findBySearchCriteria( BookingSearchCriteria bookingCriteria );

	public List<BookingSearchResponseItem> searchBookings(BookingSearchCriteria aBookingSearchCriteria );

}
