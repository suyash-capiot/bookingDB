package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;

public interface ClientCommercialRepository extends JpaRepository<ClientCommercial, Serializable> {

	public ClientCommercial saveOrder(ClientCommercial orderObj,String prevRoomDetails);
	
	
}
