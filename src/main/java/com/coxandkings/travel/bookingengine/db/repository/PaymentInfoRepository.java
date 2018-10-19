package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Serializable> {

	public PaymentInfo saveOrder(PaymentInfo orderObj,String prevRoomDetails);
	
	
}
