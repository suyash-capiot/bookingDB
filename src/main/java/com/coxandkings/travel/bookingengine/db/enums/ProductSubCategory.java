package com.coxandkings.travel.bookingengine.db.enums;

public enum ProductSubCategory {
	
	AIR("Flight"),
	HOTEL("Hotel"),
	EVENTS("Events"),
	BUS("Bus"),
	HOLIDAYS("Holidays");
 
    private String productSubCategory;
    
    ProductSubCategory(String newProductSubCat){
    	productSubCategory=newProductSubCat;
    }
    
    public String getProductSubCategory()    {
        return productSubCategory;
    }
    public static  ProductSubCategory fromString(String newStatus )  {
    	ProductSubCategory aProductSubCat = null;
        if( newStatus == null || newStatus.isEmpty() )  {
            return aProductSubCat;
        }

        for( ProductSubCategory tmpProductStatus : ProductSubCategory.values() )    {
            if( tmpProductStatus.getProductSubCategory().equalsIgnoreCase( newStatus ))  {
            	aProductSubCat = tmpProductStatus;
                break;
            }
        }
        return aProductSubCat;
    }
	}

