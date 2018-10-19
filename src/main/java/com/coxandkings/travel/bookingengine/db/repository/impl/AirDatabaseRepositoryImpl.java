package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;


import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;


@Repository
@Qualifier("Air")
public class AirDatabaseRepositoryImpl extends SimpleJpaRepository<AirOrders, Serializable> implements AirDatabaseRepository {

	public  AirDatabaseRepositoryImpl(EntityManager em) {
        super(AirOrders.class, em);
        this.em = em;
    }

	private EntityManager em;
	  
	public AirOrders saveOrder(AirOrders orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public List<AirOrders> findByBooking(Booking booking) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AirOrders> criteria = builder.createQuery(AirOrders.class);
		Root<AirOrders> root = criteria.from(AirOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("booking"), booking));
		criteria.where(p1);
		criteria.orderBy(builder.asc(root.get("createdAt")));
		return em.createQuery( criteria ).getResultList();
		
	}
	
	@Override
	public List<AirOrders> findBysuppID(String suppID) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AirOrders> criteria = builder.createQuery(AirOrders.class);
		Root<AirOrders> root = criteria.from(AirOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("supplierID"), suppID));
		criteria.where(p1);
		return em.createQuery( criteria ).getResultList();	
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AirOrders> getOrdersInRange(ZonedDateTime strtDate, ZonedDateTime enddte, String suppRef) {
	if(suppRef==null || suppRef.isEmpty())
	{
		 return  em.createQuery("SELECT e FROM AirOrders e WHERE e.createdAt BETWEEN :startDate AND :endDate").setParameter("startDate", strtDate)  
				  .setParameter("endDate", enddte).getResultList();
	}
	else 
	{
		 return  em.createQuery("SELECT e FROM AirOrders e WHERE e.supplierID = '"+ suppRef + "' AND e.createdAt BETWEEN :startDate AND :endDate").setParameter("startDate", strtDate)  
				  .setParameter("endDate", enddte).getResultList();
	}
	}
	
	public List<AirOrders> getAirGDSDetailsForGDSPNR(String gdsPNR){
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<AirOrders> query = builder.createQuery(AirOrders.class);
		Root<AirOrders> root = query.from(AirOrders.class);
		Predicate p1 = builder.and(builder.equal(root.get("GDSPNR"), gdsPNR));
		query.where(p1);
		return  em.createQuery(query).getResultList();
		
		
	} 
	
}
