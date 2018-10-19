package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.TransfersAmCl;

public interface TransfersAmClRepository extends JpaRepository<TransfersAmCl, Serializable>{
	
	public TransfersAmCl saveOrder(TransfersAmCl currentOrder, String prevOrder);
	
	public List<TransfersAmCl> findforResponseUpdate(String bookID, String cancelId,String requestType,String cancelType );
/*
	public static List<TransfersAmCl> findByEntity(String string, String orderId, String type) {
	
		return null;
	}*/
	
	public List<TransfersAmCl> findByEntity(String entityName, String string, String requestType);
}
