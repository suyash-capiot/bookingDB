package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
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

import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.repository.BusAmclRepository;

@Qualifier("BusAmCl")
@Repository
public class BusAmclRepositoryImpl extends SimpleJpaRepository<BusAmCl, Serializable> implements BusAmclRepository {

	public BusAmclRepositoryImpl(EntityManager em) {
        super(BusAmCl.class, em);
        this.em = em;
    }
	
	
	private EntityManager em;
 
	public BusAmCl saveOrder(BusAmCl currentOrder, String prevOrder) {
		return this.save(currentOrder);
		
	}


	
	
	
	@Override
	public List<BusAmCl> findByEntity(String entityName, String entityIds, String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<BusAmCl> criteria = builder.createQuery(BusAmCl.class);
		Root<BusAmCl> root = criteria.from(BusAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityIds)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
		
	
	}
	
	@Override
	public List<BusAmCl> findforResponseUpdate(String entityName, String entityID,String type , String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<BusAmCl> criteria = builder.createQuery(BusAmCl.class);
		Root<BusAmCl> root = criteria.from(BusAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		predicate.add((builder.equal(root.get("description"), type)));
		predicate.add((builder.equal(root.get("supplierCharges"), "0")));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
	}
}
