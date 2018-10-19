package com.coxandkings.travel.bookingengine.db.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;

public interface SupplierCommercialRepository extends JpaRepository<SupplierCommercial, Serializable> {

	public SupplierCommercial saveOrder(SupplierCommercial orderObj,String prevorder);
	
	
}
