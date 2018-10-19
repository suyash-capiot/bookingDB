CREATE OR REPLACE FUNCTION be_db_services_schema.productsharingsearch(
	p_productname_subtype_flavorname character varying,
	p_traveldatefrom character varying,
	p_traveldateto character varying,
	p_roomcategory character varying,
	p_roomtype character varying,
	p_gender character varying,
	p_issharable character varying)
    RETURNS refcursor AS $$

		DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
	        OPEN cur_Bookings FOR
            select
			--LuxuryType,room capacity, gender preferance, passangerName
            TBL_BOOKING.bookid as bookId, TBL_ACCOORDERS.id as orderId, TBL_ACCOOROOMDETAILS.hotelname as hotelName,
            TBL_ACCOOROOMDETAILS.citycode as cityCode, TBL_ACCOOROOMDETAILS.countrycode as countryCode, TBL_ACCOOROOMDETAILS.checkindate as checkInDate,
            TBL_ACCOOROOMDETAILS.checkoutdate as checkOutDate, TBL_ACCOOROOMDETAILS.id as roomId,
            (DATE_PART('day', to_date( checkOutDate, 'YYYY MM DD' ) ) - DATE_PART('day', to_date(checkInDate, 'YYYY MM DD' ))) as numberOfNights,
			(DATE_PART('day', to_date( checkOutDate, 'YYYY MM DD' ) ) - DATE_PART('day', to_date(checkInDate, 'YYYY MM DD' ))  + 1) as numberOfDays,
            each_paxDetail_attribute ->'paxID' as pax_ID, TBL_PASSENGERDETAILS.firstname,  TBL_PASSENGERDETAILS.lastname, TBL_PASSENGERDETAILS.email,
            each_contactdetail_attribute -> 'contactInfo'as contactinfo
            FROM
            be_db_services_schema.Booking as TBL_BOOKING
            LEFT JOIN be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
            LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOOROOMDETAILS ON TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
            CROSS JOIN jsonb_array_elements(TBL_ACCOOROOMDETAILS.paxdetails) each_paxDetail_attribute
            LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS ON TBL_PASSENGERDETAILS.passanger_id = each_paxDetail_attribute ->>'paxID'
            CROSS JOIN jsonb_array_elements(TBL_PASSENGERDETAILS.contactdetails) each_contactdetail_attribute
            where 1 = 1 and
            (	-- Productname_subtype_flavorname,Gender,isSharable
                ( p_traveldatefrom IS NULL OR TBL_ACCOOROOMDETAILS.checkindate = p_traveldatefrom)
                and ( p_traveldateto IS NULL OR TBL_ACCOOROOMDETAILS.checkoutdate =  p_traveldateto)
                --Ask Pritish to Populate data in room category. Assumtion as of now : Room category is room category name
                and ( p_roomcategory IS NULL OR UPPER (TBL_ACCOOROOMDETAILS.roomcategoryname) LIKE UPPER(p_roomcategory) )
                and ( p_roomtype IS NULL OR UPPER( TBL_ACCOOROOMDETAILS.roomtypename ) LIKE UPPER(p_roomtype) )
            )
            group by
            TBL_BOOKING.bookid, TBL_ACCOORDERS.id, TBL_ACCOOROOMDETAILS.hotelname, TBL_ACCOOROOMDETAILS.citycode,
            TBL_ACCOOROOMDETAILS.countrycode, TBL_ACCOOROOMDETAILS.checkindate,
            TBL_ACCOOROOMDETAILS.checkoutdate, TBL_ACCOOROOMDETAILS.id ,each_paxDetail_attribute, TBL_PASSENGERDETAILS.firstname,
            TBL_PASSENGERDETAILS.lastname, TBL_PASSENGERDETAILS.email, each_contactdetail_attribute ;
      RETURN cur_Bookings;
                END;

$$ LANGUAGE plpgsql;
COMMIT;

--rollback;

BEGIN;

   SELECT be_db_services_schema.productSharingSearch( null, '2018-09-20', '2018-09-22', 'Regular king', 'Double', null, null)
   FETCH ALL IN "<unnamed portal 8>" ;

COMMIT;
/*
 roomId;
 hotelName;
 hotelCategory;
 hotelSubCategory;
 hotelType;
 roomCapacity;
 country;
 city;
 checkInDate;
 checkOutDate;
 numberOfDays;
 numberOfNights;
 productCategory;
 produtSubCategory;

 bookingReferenceNo;
 orderId;
 firstName;
 lastName;
 title;
 gender;
 passengerId;
 emailId;

 countryAccessCode;
 contactNumber;
*/

--select each_attribute->'paxID' as pax_ID
--from be_db_services_schema.accoroomdetails t cross join jsonb_array_elements(t.paxdetails) each_attribute
--where t.acco_order_id = '8a98827862cd62120162ce3243e2007f'

