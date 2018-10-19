package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.UpdateLock;
import com.coxandkings.travel.bookingengine.db.repository.UpdateLockRepository;

@Repository
@Qualifier("UpdateLock")
public class UpdateLockRepositoryImpl extends SimpleJpaRepository<UpdateLock, Serializable> implements UpdateLockRepository{
	
	public  UpdateLockRepositoryImpl(EntityManager em) {
        super(UpdateLock.class, em);
        this.em = em;
    }

	private EntityManager em;
	 
	@Override
	public UpdateLock saveOrder(UpdateLock orderObj,String prevOrder) {
		return this.save(orderObj);
	}
	
	@Override
	public void deleteOrder(UpdateLock orderObj) {
		this.delete(orderObj);
	}
	
}
