package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.repository.ClientCommercialRepository;


@Repository
@Qualifier("ClientCommercial")
public class ClientCommercialRepositoryImpl extends SimpleJpaRepository<ClientCommercial, Serializable> implements ClientCommercialRepository {
	

	public  ClientCommercialRepositoryImpl(EntityManager em) {
        super(ClientCommercial.class, em);
        this.em = em;
    }

	private EntityManager em;
	

	@Override
	public ClientCommercial saveOrder(ClientCommercial orderObj, String prevOrder) {
		return this.save(orderObj);
	}
		
	
	
	
}
