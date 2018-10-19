rollback;
CREATE OR REPLACE FUNCTION be_db_services_schema.searchmergebookings(
	p_checkInDate character varying,
	p_checkOutDate character varying,
	p_roomCategory character varying,
	p_roomType character varying,
    p_hotelCode character varying)
    RETURNS refcursor AS $$
		DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
	        OPEN cur_Bookings FOR
            select
			--LuxuryType,room capacity, gender preferance, passangerName
            TBL_BOOKING.bookid as bookId, 'Accomodation' as productCategory,  TBL_ACCOORDERS.productsubcategory as productSubCategory,
            TBL_ACCOORDERS.id as orderId, TBL_ACCOOROOMDETAILS.citycode as cityCode, TBL_ACCOOROOMDETAILS.countrycode as countryCode,
            TBL_ACCOOROOMDETAILS.hotelcode as hotelCode, TBL_ACCOOROOMDETAILS.hotelname as hotelName, TBL_ACCOOROOMDETAILS.id as roomId,
            TBL_ACCOOROOMDETAILS.checkindate as checkInDate, TBL_ACCOOROOMDETAILS.checkoutdate as checkOutDate, each_paxDetail_attribute ->'paxID' as paxID,
            TBL_PASSENGERDETAILS.isleadpax as isLeadPax, TBL_ACCOOROOMDETAILS.roomtypecode as roomTypeCode, TBL_ACCOOROOMDETAILS.roomcategoryid as roomCategoryId,
            TBL_ACCOOROOMDETAILS.roomref as roomRef, TBL_ACCOOROOMDETAILS.roomtypename as roomTypeName, TBL_ACCOOROOMDETAILS.roomCategoryName as roomCategoryName,
            TBL_ACCOOROOMDETAILS.mealcode as mealCode, TBL_ACCOOROOMDETAILS.mealname as mealName, TBL_ACCOOROOMDETAILS.totalprice as totalPrice,
            TBL_ACCOOROOMDETAILS.totaltaxbreakup as totalTaxBreakup, TBL_ACCOOROOMDETAILS.totalpricecurrencycode as totalPriceCurrencyCode,
            TBL_ACCOOROOMDETAILS.suppliertaxbreakup as supplierTaxBreakup,
            TBL_ACCOOROOMDETAILS.supplierprice as supplierPrice, TBL_ACCOOROOMDETAILS.supplierpricecurrencycode as supplierPriceCurrencyCode
            FROM
            be_db_services_schema.Booking as TBL_BOOKING
            LEFT JOIN be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
            LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOOROOMDETAILS ON TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
            CROSS JOIN jsonb_array_elements(TBL_ACCOOROOMDETAILS.paxdetails) each_paxDetail_attribute
            LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS ON TBL_PASSENGERDETAILS.passanger_id = each_paxDetail_attribute ->>'paxID'
            where 1 = 1 and
            (
                ( p_checkInDate IS NULL OR TBL_ACCOOROOMDETAILS.checkindate = p_checkInDate)
                and ( p_checkOutDate IS NULL OR TBL_ACCOOROOMDETAILS.checkoutdate =  p_checkOutDate)
                --Ask Pritish to Populate data in room category. Assumtion as of now : Room category is room category name
                and ( p_roomcategory IS NULL OR UPPER (TBL_ACCOOROOMDETAILS.roomcategoryname) LIKE UPPER(p_roomcategory) )
                and ( p_roomtype IS NULL OR UPPER( TBL_ACCOOROOMDETAILS.roomtypename ) LIKE UPPER(p_roomtype) )
                and ( p_hotelCode IS NULL OR UPPER( TBL_ACCOOROOMDETAILS.hotelcode ) LIKE UPPER(p_hotelCode) )
            )
            group by
             TBL_BOOKING.bookid, productCategory, productSubCategory, orderId, cityCode, countryCode, hotelCode, hotelName, roomId,
            checkInDate, checkOutDate, paxID, isLeadPax, roomTypeCode, roomCategoryId, roomRef,  roomTypeName, roomCategoryName,
            mealCode,  mealName, TBL_ACCOOROOMDETAILS.totalprice, TBL_ACCOOROOMDETAILS.totaltaxbreakup,  TBL_ACCOOROOMDETAILS.totalpricecurrencycode,
            supppriceTaxes, TBL_ACCOOROOMDETAILS.supplierprice, TBL_ACCOOROOMDETAILS.supplierpricecurrencycode;
      RETURN cur_Bookings;
                END;

$$ LANGUAGE plpgsql;
COMMIT;

--rollback;

BEGIN;
   SELECT be_db_services_schema.searchmergebookings( null, null,'Regular king', 'Double', null)
   --SELECT be_db_services_schema.productSharingSearch( null, '2018-09-20', '2018-09-22', 'Regular king', 'Double', null, null)
   FETCH ALL IN "<unnamed portal 13>" ;

COMMIT;
--/*
--1.	Booking ref No.
--2.	Order Object
--a.	Product category
--b.	Product Sub category
--c.	Order id
--d.	Order details
--i.	Hotel Details
--1.	City code
--2.	Country code
--3.	Hotel code
--4.	Hotel name
--5.	Rooms
--a.	Room id
--b.	Check in date
--c.	Check out date
--d.	Pax info
--e.	Room type info
--f.	Meal info
--ii.	Order total price info
--iii.	Order supplier price info
--
--*/

--select each_attribute->'paxID' as pax_ID
--from be_db_services_schema.accoroomdetails t cross join jsonb_array_elements(t.paxdetails) each_attribute
--where t.acco_order_id = '8a98827862cd62120162ce3243e2007f'


