package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


import com.coxandkings.travel.bookingengine.db.model.BusAmCl;


public interface BusAmclRepository extends JpaRepository<BusAmCl, Serializable>{
	
	public BusAmCl saveOrder(BusAmCl currentOrder, String prevOrder);
	
	
	public List<BusAmCl> findByEntity(String entityName, String orderId, String requestType) ;
	
	public List<BusAmCl> findforResponseUpdate(String entityName, String entityID,String requestType,String type );

}
