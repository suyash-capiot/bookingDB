package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.ActivitiesDatabaseRepository;

@Qualifier("Activity")
@Repository
public class ActivitiesDatabaseRepositoryImpl  extends SimpleJpaRepository<ActivitiesOrders, Serializable> implements ActivitiesDatabaseRepository{

	public ActivitiesDatabaseRepositoryImpl(EntityManager em) {
		 super(ActivitiesOrders.class, em);
	        this.em = em;
	}
	private EntityManager em; 

	
	// TODO : Not performing any operation with prevOrder currently 
	@Override
	public ActivitiesOrders saveOrder(ActivitiesOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}

	@Override
	public List<ActivitiesOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<ActivitiesOrders> criteria = builder.createQuery(ActivitiesOrders.class);
		Root<ActivitiesOrders> root = criteria.from(ActivitiesOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		// TODO : As its breaking getBooking I am commenting it
		//		return em.createQuery( criteria ).getResultList();
		return em.createQuery( criteria ).getResultList();
	}

	@Override
	public List<ActivitiesOrders> findOrderID(String supp_Booking_refrence) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<ActivitiesOrders> criteria = builder.createQuery(ActivitiesOrders.class);
		Root<ActivitiesOrders> root = criteria.from(ActivitiesOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supp_booking_reference"), supp_Booking_refrence));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();
	}

}
