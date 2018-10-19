package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.repository.PaymentInfoRepository;

@Repository
@Qualifier("PaymentInfo")
public class PaymentInfoRepositoryImpl extends SimpleJpaRepository<PaymentInfo, Serializable> implements PaymentInfoRepository{
	
	public  PaymentInfoRepositoryImpl(EntityManager em) {
        super(PaymentInfo.class, em);
        this.em = em;
    }

	private EntityManager em;
	

	@Override
	public PaymentInfo saveOrder(PaymentInfo orderObj, String prevOrder) {
		return this.save(orderObj);
	}

}
