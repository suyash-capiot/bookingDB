package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.SupplierCommercialRepository;

//TODO: if we decide not to use it, remove this class.

@Repository
@Qualifier("SuppCommercial")
public class SupplierCommercialRepositoryImpl extends SimpleJpaRepository<SupplierCommercial, Serializable> implements SupplierCommercialRepository{
	
	public  SupplierCommercialRepositoryImpl(EntityManager em) {
        super(SupplierCommercial.class, em);
        this.em = em;
    }

	private EntityManager em;
	

	@Override
	public SupplierCommercial saveOrder(SupplierCommercial orderObj, String prevOrder) {
		return this.save(orderObj);
	}
	
}
