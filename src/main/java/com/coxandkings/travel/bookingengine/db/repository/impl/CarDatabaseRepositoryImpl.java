package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;


@Repository
@Qualifier("Car")
public class CarDatabaseRepositoryImpl extends SimpleJpaRepository<CarOrders, Serializable> implements CarDatabaseRepository {

	public  CarDatabaseRepositoryImpl(EntityManager em) {
        super(CarOrders.class, em);
        this.em = em;
    }

	private EntityManager em;
	  
	public CarOrders saveOrder(CarOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<CarOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarOrders> criteria = builder.createQuery(CarOrders.class);
		Root<CarOrders> root = criteria.from(CarOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		criteria.orderBy(builder.asc(root.get("createdAt")));
		return em.createQuery( criteria ).getResultList();
		
	}
	
	@Override
	public List<CarOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarOrders> criteria = builder.createQuery(CarOrders.class);
		Root<CarOrders> root = criteria.from(CarOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}

	@Override
	public List<CarOrders> findBySuppReservationId(String suppId, String id) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CarOrders> criteria = builder.createQuery(CarOrders.class);
		Root<CarOrders> root = criteria.from(CarOrders.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add(builder.equal(root.get("supplierID"), suppId));
		predicate.add(builder.equal(root.get("carReservationId"), id));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		criteria.orderBy(builder.asc(root.get("bookingDateTime")));
		return em.createQuery( criteria ).getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CarOrders> getOrdersInRange(ZonedDateTime strtDate, ZonedDateTime enddte, String suppRef) {
			
		if(suppRef==null || suppRef.isEmpty()){
			 return  em.createQuery("SELECT e FROM CarOrders e WHERE e.createdAt BETWEEN :startDate AND :endDate").setParameter("startDate", strtDate)  
					  .setParameter("endDate", enddte).getResultList();
		}
		else {
			 return  em.createQuery("SELECT e FROM CarOrders e WHERE e.supplierID = '"+ suppRef + "' AND e.createdAt BETWEEN :startDate AND :endDate").setParameter("startDate", strtDate)  
					  .setParameter("endDate", enddte).getResultList();
		}
	}
}
