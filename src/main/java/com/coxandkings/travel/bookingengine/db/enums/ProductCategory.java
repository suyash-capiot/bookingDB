package com.coxandkings.travel.bookingengine.db.enums;

import org.springframework.util.StringUtils;

public enum ProductCategory {

    PRODUCT_CATEGORY_TRANSPORTATION( "Transportation" ),
    PRODUCT_CATEGORY_ACCOMMODATION( "Accommodation" ),
    PRODUCT_CATEGORY_ACTIVITIES( "Activities" ),
	PRODUCT_CATEGORY_HOLIDAYS( "Holidays" );

    private String category;

    ProductCategory(String newCategory )   {
        category = newCategory;
    }

    public static ProductCategory getProductCategory(String aCategory) {
        ProductCategory productCategory = null;
        if(StringUtils.isEmpty(aCategory)) {
            return null;
        }

        for(ProductCategory opsTmpProductCategory: ProductCategory.values()) {
            if(opsTmpProductCategory.getCategory().equalsIgnoreCase(aCategory)) {
                productCategory = opsTmpProductCategory;
                break;
            }
        }

        return productCategory;
    }

    public String getCategory() {
        return category;
    }
}
