package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.UpdateLock;


public interface UpdateLockRepository extends JpaRepository<UpdateLock, Serializable> {

	public UpdateLock saveOrder(UpdateLock orderObj, String prevOrder);
	
	public void deleteOrder(UpdateLock orderObj);
	
}
