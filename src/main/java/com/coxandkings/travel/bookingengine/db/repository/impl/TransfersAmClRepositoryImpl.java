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
import com.coxandkings.travel.bookingengine.db.model.TransfersAmCl;
import com.coxandkings.travel.bookingengine.db.repository.TransfersAmClRepository;


@Qualifier("TransfersAmCl")
@Repository
public class TransfersAmClRepositoryImpl extends SimpleJpaRepository<TransfersAmCl, Serializable> implements TransfersAmClRepository {

	public TransfersAmClRepositoryImpl(EntityManager em) {
        super(TransfersAmCl.class, em);
        this.em = em;
    }
	
	
	private EntityManager em;
 
	public TransfersAmCl saveOrder(TransfersAmCl currentOrder, String prevOrder) {
		return this.save(currentOrder);
		
	}

	@Override
	public List<TransfersAmCl> findforResponseUpdate(String entityName, String entityID,String type , String requestType) {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TransfersAmCl> criteria = builder.createQuery(TransfersAmCl.class);
		Root<TransfersAmCl> root = criteria.from(TransfersAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityID)));
		/*predicate.add( (builder.equal(root.get("bookId"), bookID)));
//		predicate.add((builder.equal(root.get("id"), cancelId)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		predicate.add((builder.equal(root.get("cancelType"), cancelType)));
//		predicate.add((builder.equal(root.get("supplierCharges"), "0")));
 * 
*/	
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		predicate.add((builder.equal(root.get("description"), type)));
		predicate.add((builder.equal(root.get("supplierCharges"), "0")));
		criteria.where(builder.and(predicate.toArray(new Predicate[predicate.size()] )));
	
		return em.createQuery( criteria ).getResultList();
		
	}

	public List<TransfersAmCl> findByEntity(String entityName, String entityIds, String requestType) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TransfersAmCl> criteria = builder.createQuery(TransfersAmCl.class);
		Root<TransfersAmCl> root = criteria.from(TransfersAmCl.class);
		List<Predicate> predicate = new ArrayList<Predicate>();
		/*predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("orderId"), orderId)));
	//	predicate.add((builder.equal(root.get("type"), type)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));*/
		predicate.add( (builder.equal(root.get("entityName"), entityName)));
		predicate.add((builder.equal(root.get("entityID"), entityIds)));
		predicate.add((builder.equal(root.get("requestType"), requestType)));
		
		criteria.where(builder.and(predicate.toArray(new Predicate[] {})));
		
		return em.createQuery( criteria ).getResultList();
	}
	
}
