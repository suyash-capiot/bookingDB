package com.coxandkings.travel.bookingengine.db.orchestrator;

public interface HolidaysComponentsCommonMethods {

	String getSupplierPriceBeforeTax();
	
	String getSupplierPriceAfterTax();

	String getSupplierPriceCurrencyCode();

	String getSupplierTaxBreakup();
	
	String getSupplierPaxTypeFares();

	String getSupplierTaxAmount();

	String getTotalPriceBeforeTax();

	String getTotalPriceAfterTax();
	
	String getTotalPriceCurrencyCode();

	String getTotalTaxBreakup();
	
	String getTotalPaxTypeFares();

	String getTotalTaxAmount();
	
	String getStart();
	
	String getDuration();
	
	String getEnd();

	String getPaxDetails();

	String getReceivables();
}
