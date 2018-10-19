--- First Run DROP Function of SearchBookings ---
DROP FUNCTION be_db_services_schema.SearchBookings(
    p_startIndex IN INTEGER,
    p_endIndex IN INTEGER,
    p_sortField IN VARCHAR,
    p_BookingSearchCriteria IN be_db_services_schema.BookingSearchCriteria,
	p_CompaniesSearchCriteria IN be_db_services_schema.CompaniesSearchCriteria,
	p_ClientPaxSearchCriteria IN be_db_services_schema.ClientPaxSearchCriteria,
    p_FlightSearchCriteria IN be_db_services_schema.FlightSearchCriteria,
	p_HotelSearchCriteria IN be_db_services_schema.HotelSearchCriteria,
	p_activitiesSearchCriteria IN be_db_services_schema.ActivitiesSearchCriteria,
	p_HolidaysSearchCriteria IN be_db_services_schema.HolidaysSearchCriteria
) cascade;

DROP TYPE be_db_services_schema.BookingSearchCriteria;
DROP TYPE be_db_services_schema.CompaniesSearchCriteria;
DROP TYPE be_db_services_schema.FlightSearchCriteria;
DROP TYPE be_db_services_schema.HotelSearchCriteria;
DROP TYPE be_db_services_schema.ClientPaxSearchCriteria;
DROP TYPE be_db_services_schema.ActivitiesSearchCriteria;
DROP TYPE be_db_services_schema.HolidaysSearchCriteria;
COMMIT;

CREATE TYPE be_db_services_schema.BookingSearchCriteria AS (
    BookingID varchar, BookingTypeId varchar, BookingFromDate timestamp,
    BookingToDate timestamp, priority varchar, BookingStatus varchar,
    AssignmentStatus Boolean , UserID varchar, FinancialControlId varchar
);

CREATE TYPE be_db_services_schema.CompaniesSearchCriteria AS	(
	GroupOfCompaniesId varchar, GroupNameId varchar, CompanyId varchar,
	CompanyMarketId varchar, SBUId varchar, BUId varchar
);

CREATE TYPE be_db_services_schema.FlightSearchCriteria AS (
    ProductSubCategory varchar, SupplierName varchar, AirlinePNR varchar,
    TravelFromDate  timestamp, TravelToDate  timestamp, GdsPNR varchar,
    TicketNumber varchar,AirlineName varchar
);

CREATE TYPE be_db_services_schema.HotelSearchCriteria AS (
	ProductSubCategory varchar, Country varchar, City varchar,
    ProductName varchar, SupplierReferenceNumber varchar, SupplierName varchar
);

CREATE TYPE be_db_services_schema.ClientPaxSearchCriteria AS (
	ClientType varchar, ClientCategory varchar, ClientSubCategory varchar,
	ClientId varchar, PaxName varchar, PhoneNumber varchar,
    EmailID varchar
);

CREATE TYPE be_db_services_schema.ActivitiesSearchCriteria AS (
	ProductSubCategory varchar, Country varchar, City varchar,
    ProductName varchar, SupplierReferenceNumber varchar, SupplierName varchar
);

CREATE TYPE be_db_services_schema.HolidaysSearchCriteria AS (
	ProductSubCategory varchar, Destination varchar , Country varchar, City varchar,
    NoOfNights varchar, ProductName varchar, ProductFlavourName varchar, PackageType varchar,
	Brand varchar, CompanyPackageName varchar, SupplierReferenceNumber varchar, SupplierName varchar
);

COMMIT;

CREATE OR REPLACE FUNCTION be_db_services_schema.searchbookings(
	p_startindex integer,
	p_limit integer,
	p_sortfield character varying,
	p_bookingsearchcriteria be_db_services_schema.bookingsearchcriteria,
	p_companiessearchcriteria be_db_services_schema.companiessearchcriteria,
	p_clientpaxsearchcriteria be_db_services_schema.clientpaxsearchcriteria,
	p_flightsearchcriteria be_db_services_schema.flightsearchcriteria,
	p_hotelsearchcriteria be_db_services_schema.hotelsearchcriteria,
	p_activitiessearchcriteria be_db_services_schema.activitiessearchcriteria,
	p_holidayssearchcriteria be_db_services_schema.holidayssearchcriteria)
    RETURNS refcursor AS $$

	DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
    BEGIN
	        -- Print logs --
        	-- RAISE NOTICE 'The input Booking ID is: --> %', p_BookingSearchCriteria.bookingRefId;
        	-- End of logs
            OPEN cur_Bookings FOR
            select
            TBL_BOOKING.bookid, TBL_BOOKING.createdat, TBL_BOOKING.clienttype, TBL_BOOKING.clientid, TBL_BOOKING.companyid, TBL_BOOKING.status,
            TBL_BOOKING.staffid, TBL_BOOKING.pos as pointofsale, TBL_PAYMENTINFO.paymenttype as paymentstatus,
			TBL_AIRORDERS.productsubcategory as flightSubCategory,
			TBL_ACCOORDERS.productsubcategory as hotelSubCategory,
            TBL_ACTIVITIESORDERS.productSubCategory as activitiessubcategory,
			TBL_HOLIDAYSORDERS.productSubCategory as holidayssubcategory,
			count(*) OVER() AS fullcount
            FROM
            be_db_services_schema.Booking as TBL_BOOKING
            LEFT JOIN be_db_services_schema.airorders as TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
			LEFT JOIN be_db_services_schema.paymentinfo as TBL_PAYMENTINFO ON TBL_PAYMENTINFO.booking_id = TBL_BOOKING.bookid
            LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.flightdetails->'originDestinationOptions') each_odo ON TRUE
            LEFT JOIN LATERAL jsonb_array_elements(each_odo -> 'flightSegment') each_flight_segment ON TRUE
			LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.paxdetails) each_flight_paxdetail_attribute ON TRUE
			LEFT JOIN be_db_services_schema.passengerdetails as TBL_FLIGHT_PASSENGERDETAILS ON TBL_FLIGHT_PASSENGERDETAILS.passanger_id = each_flight_paxdetail_attribute ->>'paxID'
			LEFT JOIN LATERAL jsonb_array_elements(TBL_FLIGHT_PASSENGERDETAILS.contactdetails) each_flight_contactinfo_attribute ON TRUE

			LEFT JOIN be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
            LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOOROOMDETAILS ON TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
			LEFT JOIN LATERAL jsonb_array_elements(TBL_ACCOOROOMDETAILS.paxdetails) each_room_paxdetail_attribute ON TRUE
			LEFT JOIN be_db_services_schema.passengerdetails as TBL_ACCO_PASSENGERDETAILS ON TBL_ACCO_PASSENGERDETAILS.passanger_id = each_room_paxdetail_attribute ->>'paxID'
			LEFT JOIN LATERAL jsonb_array_elements(TBL_ACCO_PASSENGERDETAILS.contactdetails) each_acco_contactinfo_attribute ON TRUE

			LEFT JOIN be_db_services_schema.ACTIVITIESORDERS as TBL_ACTIVITIESORDERS ON TBL_ACTIVITIESORDERS.bookid = TBL_BOOKING.bookid
			LEFT JOIN LATERAL jsonb_array_elements(TBL_ACTIVITIESORDERS.paxdetails) each_activities_paxdetail_attribute ON TRUE
			LEFT JOIN be_db_services_schema.passengerdetails as TBL_ACTIVITIES_PASSENGERDETAILS ON TBL_ACTIVITIES_PASSENGERDETAILS.passanger_id = each_activities_paxdetail_attribute ->>'paxID'
			LEFT JOIN LATERAL jsonb_array_elements(TBL_ACTIVITIES_PASSENGERDETAILS.contactdetails) each_activities_contactinfo_attribute ON TRUE

			LEFT JOIN be_db_services_schema.holidaysorders as TBL_HOLIDAYSORDERS ON TBL_HOLIDAYSORDERS.bookid = TBL_BOOKING.bookid
			LEFT JOIN LATERAL jsonb_array_elements(TBL_HOLIDAYSORDERS.paxdetails) each_holidays_paxdetail_attribute ON TRUE
			LEFT JOIN be_db_services_schema.passengerdetails as TBL_HOLIDAYS_PASSENGERDETAILS ON TBL_HOLIDAYS_PASSENGERDETAILS.passanger_id = each_holidays_paxdetail_attribute ->>'paxID'
		    LEFT JOIN LATERAL jsonb_array_elements(TBL_HOLIDAYS_PASSENGERDETAILS.contactdetails) each_holidays_contactinfo_attribute ON TRUE

			LEFT JOIN be_db_services_schema.accoorders as TBL_ACCOORDERS2 ON TBL_ACCOORDERS2.holidays_order_id = TBL_HOLIDAYSORDERS.id
            LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOOROOMDETAILS2 ON TBL_ACCOOROOMDETAILS2.acco_order_id = TBL_ACCOORDERS.id
			LEFT JOIN be_db_services_schema.activitiesorders as TBL_ACTIVITIESORDERS2 ON TBL_ACTIVITIESORDERS2.holidays_order_id = TBL_HOLIDAYSORDERS.id
			LEFT JOIN be_db_services_schema.insuranceorders as TBL_INSURANCEORDERS2 ON TBL_INSURANCEORDERS2.holidays_order_id = TBL_HOLIDAYSORDERS.id
			LEFT JOIN be_db_services_schema.transfersorders as TBL_TRANSFERSORDERS2 ON TBL_TRANSFERSORDERS2.holidays_order_id = TBL_HOLIDAYSORDERS.id
			LEFT JOIN be_db_services_schema.holidaysextensiondetails as TBL_HOLIDAYSEXTENSIONDETAILS ON TBL_HOLIDAYSEXTENSIONDETAILS.holidays_order_id = TBL_HOLIDAYSORDERS.id
			LEFT JOIN be_db_services_schema.holidaysextrasdetails as TBL_HOLIDAYSEXTRASDETAILS ON TBL_HOLIDAYSEXTRASDETAILS.holidays_order_id = TBL_HOLIDAYSORDERS.id


			where 1 = 1 and
			(
				 -- Search Param missing Booking Type, Prioroty, Financial Control Id
                ( p_BookingSearchCriteria.BookingID IS NULL OR  UPPER(TBL_BOOKING.bookid) LIKE UPPER (p_BookingSearchCriteria.BookingID))
                and ( p_BookingSearchCriteria.BookingFromDate IS NULL OR date(TBL_BOOKING.createdat) >= date(p_BookingSearchCriteria.BookingFromDate))
                and ( p_BookingSearchCriteria.BookingToDate IS NULL OR date(TBL_BOOKING.createdat) <= date(p_BookingSearchCriteria.BookingToDate))
                and ( p_BookingSearchCriteria.BookingStatus IS NULL OR UPPER(TBL_BOOKING.status) like UPPER(p_BookingSearchCriteria.BookingStatus))
                --and ( p_BookingSearchCriteria.priority IS NULL OR TBL_BOOKING.?? = p_BookingSearchCriteria.priority )
                and ( p_BookingSearchCriteria.UserID IS NULL OR TBL_BOOKING.staffid = p_BookingSearchCriteria.UserID )
 				and (
 					   ( p_bookingsearchcriteria.AssignmentStatus IS NULL) OR
 					   ( p_BookingSearchCriteria.AssignmentStatus IS false and TBL_BOOKING.staffid IS NULL) OR
 					   ( p_BookingSearchCriteria.AssignmentStatus IS true and TBL_BOOKING.staffid IS NOT NULL )
 				    )
				and (p_BookingSearchCriteria.BookingTypeId IS NULL OR UPPER(TBL_BOOKING.bookingtype) LIKE UPPER (p_BookingSearchCriteria.BookingTypeId)
					OR p_BookingSearchCriteria.BookingTypeId = 'Both')
		    )
            and
			(
                (
                    -- Search params!!
                    -- AirlineName varchar, Supplier Reference Number / PNR
                    ( UPPER( TBL_AIRORDERS.productsubcategory) LIKE UPPER(p_FlightSearchCriteria.ProductSubCategory))
                    and ( p_FlightSearchCriteria.AirlinePNR IS NULL OR UPPER( TBL_AIRORDERS.airlinepnr) like UPPER( p_FlightSearchCriteria.AirlinePNR ) )
                    and ( p_FlightSearchCriteria.GdsPNR IS NULL OR UPPER( TBL_AIRORDERS.gdspnr ) like UPPER( p_FlightSearchCriteria.GdsPNR ) )
                    and ( p_FlightSearchCriteria.TravelFromDate IS NULL OR  ( date(each_flight_segment ->> 'departureDate')  >= date( p_FlightSearchCriteria.TravelFromDate) ) )
                    and ( p_FlightSearchCriteria.TravelToDate IS NULL OR   ( date(each_flight_segment ->>'arrivalDate')  <= date( p_FlightSearchCriteria.TravelToDate) ) )
                    --TestTicketPnr is ticktetingpnr not yet matched
					and ( p_FlightSearchCriteria.TicketNumber IS NULL OR UPPER( TBL_AIRORDERS.ticketnumber ) like UPPER( p_FlightSearchCriteria.TicketNumber ) )
                    and ( p_FlightSearchCriteria.SupplierName IS NULL OR UPPER(TBL_AIRORDERS.supplierId) like UPPER( p_FlightSearchCriteria.SupplierName ) )
					and (p_clientpaxsearchcriteria.PaxName IS NULL
						 OR UPPER(TBL_FLIGHT_PASSENGERDETAILS.firstname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%')
						 OR UPPER(TBL_FLIGHT_PASSENGERDETAILS.lastname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%') )
					and ( p_clientpaxsearchcriteria.EmailID IS NULL
						 OR UPPER( TBL_FLIGHT_PASSENGERDETAILS.email) LIKE UPPER(p_clientpaxsearchcriteria.EmailID)
						 OR UPPER (each_flight_contactinfo_attribute->'contactInfo'->>'email') LIKE UPPER(p_clientpaxsearchcriteria.EmailID))
					and (p_clientpaxsearchcriteria.PhoneNumber IS NULL OR each_flight_contactinfo_attribute->'contactInfo'->>'mobileNo' = p_clientpaxsearchcriteria.PhoneNumber)
                )
                or
                (	-- BEGIN Hotel details Filter criteria
				    -- ProductName
                    (UPPER (TBL_ACCOORDERS.productsubcategory) LIKE UPPER( p_HotelSearchCriteria.ProductSubCategory))
                    and ( p_HotelSearchCriteria.Country IS NULL OR UPPER(TBL_ACCOOROOMDETAILS.countrycode) LIKE UPPER( p_HotelSearchCriteria.Country ))
                    and ( p_HotelSearchCriteria.City IS NULL OR UPPER(TBL_ACCOOROOMDETAILS.citycode) LIKE UPPER( p_HotelSearchCriteria.City ))
                    --Assumption: Supplier Name Mapped with Supplier ID Assumption--
                    and ( p_HotelSearchCriteria.SupplierName IS NULL OR UPPER(TBL_ACCOORDERS.supplierid) LIKE UPPER( p_HotelSearchCriteria.SupplierName ))
                    and ( p_HotelSearchCriteria.SupplierReferenceNumber IS NULL OR UPPER( TBL_ACCOORDERS.supplierreferenceid ) LIKE UPPER( p_HotelSearchCriteria.SupplierReferenceNumber ) )
					and (p_clientpaxsearchcriteria.PaxName IS NULL
						 OR UPPER(TBL_ACCO_PASSENGERDETAILS.firstname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%')
						 OR UPPER(TBL_ACCO_PASSENGERDETAILS.lastname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%') )
						 and ( p_clientpaxsearchcriteria.EmailID IS NULL OR UPPER(TBL_ACCO_PASSENGERDETAILS.email) LIKE UPPER(p_clientpaxsearchcriteria.EmailID)
							 OR UPPER (each_acco_contactinfo_attribute->'contactInfo'->>'email') LIKE UPPER(p_clientpaxsearchcriteria.EmailID))
						 and ( p_clientpaxsearchcriteria.PhoneNumber IS NULL OR each_acco_contactinfo_attribute->'contactInfo'->>'mobileNo' = p_clientpaxsearchcriteria.PhoneNumber)
				)
                or
                (
                    ( UPPER( TBL_ACTIVITIESORDERS.productsubcategory) LIKE UPPER(p_activitiesSearchCriteria.ProductSubCategory))
                    and ( p_activitiesSearchCriteria.Country IS NULL OR UPPER( TBL_ACTIVITIESORDERS.countrycode) like UPPER( p_activitiesSearchCriteria.Country ) )
                    and ( p_activitiesSearchCriteria.City IS NULL OR UPPER( TBL_ACTIVITIESORDERS.citycode ) like UPPER( p_activitiesSearchCriteria.City ) )
                    --Assumption ProductName is name
                    and ( p_activitiesSearchCriteria.ProductName IS NULL OR UPPER( TBL_ACTIVITIESORDERS.name ) like UPPER( p_activitiesSearchCriteria.ProductName )   )
                    and ( p_activitiesSearchCriteria.SupplierReferenceNumber IS NULL OR   UPPER( TBL_ACTIVITIESORDERS.supp_booking_reference ) like UPPER( p_activitiesSearchCriteria.SupplierReferenceNumber )  )
                    and ( p_activitiesSearchCriteria.SupplierName IS NULL OR UPPER( TBL_ACTIVITIESORDERS.supplierid ) like UPPER( p_activitiesSearchCriteria.SupplierName ) )
					and (p_clientpaxsearchcriteria.PaxName IS NULL
						 OR UPPER(TBL_ACTIVITIES_PASSENGERDETAILS.firstname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%')
						 OR UPPER(TBL_ACTIVITIES_PASSENGERDETAILS.lastname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%') )
						 and ( p_clientpaxsearchcriteria.EmailID IS NULL
						   OR UPPER ( TBL_ACTIVITIES_PASSENGERDETAILS.email) LIKE UPPER(p_clientpaxsearchcriteria.EmailID)
						   OR UPPER ( each_activities_contactinfo_attribute->'contactInfo'->>'email' ) LIKE UPPER(p_clientpaxsearchcriteria.EmailID))
					 and (p_clientpaxsearchcriteria.PhoneNumber IS NULL OR each_activities_contactinfo_attribute->'contactInfo'->>'mobileNo' = p_clientpaxsearchcriteria.PhoneNumber)
                )
                or
                (
                    -- BEGIN Holidays details Filter criteria
                    (UPPER (TBL_HOLIDAYSORDERS.productsubcategory) LIKE UPPER( p_HolidaysSearchCriteria.ProductSubCategory))
                    and ( p_HolidaysSearchCriteria.Destination IS NULL OR UPPER(TBL_HOLIDAYSORDERS.destination) LIKE UPPER( p_HolidaysSearchCriteria.Destination ))
                    and ( p_HolidaysSearchCriteria.Country IS NULL OR UPPER(TBL_HOLIDAYSORDERS.country) LIKE UPPER( p_HolidaysSearchCriteria.Country ))
                    and ( p_HolidaysSearchCriteria.City IS NULL OR UPPER(TBL_HOLIDAYSORDERS.city) LIKE UPPER( p_HolidaysSearchCriteria.City ))
                    and ( p_HolidaysSearchCriteria.NoOfNights IS NULL OR UPPER(TBL_HOLIDAYSORDERS.noOfNights) LIKE UPPER( p_HolidaysSearchCriteria.NoOfNights ))
                    and ( p_HolidaysSearchCriteria.ProductName IS NULL OR UPPER(TBL_HOLIDAYSORDERS.productName) LIKE UPPER( p_HolidaysSearchCriteria.ProductName ))
                    and ( p_HolidaysSearchCriteria.ProductFlavourName IS NULL OR UPPER(TBL_HOLIDAYSORDERS.productFlavourName) LIKE UPPER( p_HolidaysSearchCriteria.ProductFlavourName ))
                    and ( p_HolidaysSearchCriteria.PackageType IS NULL OR UPPER(TBL_HOLIDAYSORDERS.packageType) LIKE UPPER( p_HolidaysSearchCriteria.PackageType ))
                    and ( p_HolidaysSearchCriteria.Brand IS NULL OR UPPER(TBL_HOLIDAYSORDERS.brandName) LIKE UPPER( p_HolidaysSearchCriteria.Brand ))
                    and ( p_HolidaysSearchCriteria.CompanyPackageName IS NULL OR UPPER(TBL_HOLIDAYSORDERS.companyPackageName) LIKE UPPER( p_HolidaysSearchCriteria.CompanyPackageName ))
                    and ( p_HolidaysSearchCriteria.SupplierReferenceNumber IS NULL OR UPPER(TBL_HOLIDAYSORDERS.supplierReferenceNumber) LIKE UPPER( p_HolidaysSearchCriteria.SupplierReferenceNumber ))
                    and ( p_HolidaysSearchCriteria.SupplierName IS NULL OR UPPER(TBL_HOLIDAYSORDERS.supplierName) LIKE UPPER( p_HolidaysSearchCriteria.SupplierName ))
					and (p_clientpaxsearchcriteria.PaxName IS NULL
						 OR UPPER(TBL_HOLIDAYS_PASSENGERDETAILS.firstname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%')
						 OR UPPER(TBL_HOLIDAYS_PASSENGERDETAILS.lastname) LIKE UPPER ('%' || p_clientpaxsearchcriteria.PaxName || '%') )
					 and ( p_clientpaxsearchcriteria.EmailID IS NULL
						  OR UPPER(TBL_HOLIDAYS_PASSENGERDETAILS.email) LIKE UPPER (p_clientpaxsearchcriteria.EmailID)
						  OR UPPER (each_holidays_contactinfo_attribute->'contactInfo'->>'email') LIKE UPPER(p_clientpaxsearchcriteria.EmailID))
					 and (p_clientpaxsearchcriteria.PhoneNumber IS NULL OR each_holidays_contactinfo_attribute->'contactInfo'->>'mobileNo' = p_clientpaxsearchcriteria.PhoneNumber)
                )
                )
            and
            (
				-- Check with Pritish on BE Table column Company Market
                ( p_CompaniesSearchCriteria.GroupOfCompaniesId IS NULL OR TBL_BOOKING.groupofcomapniesid >= p_CompaniesSearchCriteria.GroupOfCompaniesId)
				and ( p_CompaniesSearchCriteria.GroupNameId IS NULL OR TBL_BOOKING.groupcompanyid = p_CompaniesSearchCriteria.GroupNameId )
                and ( p_CompaniesSearchCriteria.CompanyId IS NULL OR TBL_BOOKING.companyid = p_CompaniesSearchCriteria.CompanyId )
				--and ( p_CompaniesSearchCriteria.CompanyMarketId IS NULL OR TBL_BOOKING.createdat >= p_CompaniesSearchCriteria.CompanyMarketId)
				and ( p_CompaniesSearchCriteria.SBUId IS NULL OR TBL_BOOKING.sbu LIKE UPPER(p_CompaniesSearchCriteria.SBUId))
				and ( p_CompaniesSearchCriteria.BUId IS NULL OR TBL_BOOKING.bu LIKE UPPER(p_CompaniesSearchCriteria.BUId))
            )
			and
            (
                --ClientCategory varchar, Client SubCategory varchar, ClientName varchar, PassengerName varchar(Air and Acco Done), PhoneNumber varchar
                (p_clientpaxsearchcriteria.ClientType IS NULL OR UPPER(TBL_BOOKING.clienttype) LIKE UPPER(  p_clientpaxsearchcriteria.ClientType))
                 and (p_clientpaxsearchcriteria.ClientId IS NULL OR UPPER(TBL_BOOKING.clientId) LIKE UPPER (p_clientpaxsearchcriteria.ClientId))
		    )

            --and TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
			group by
            TBL_BOOKING.bookid, TBL_BOOKING.createdat, TBL_BOOKING.clienttype, TBL_BOOKING.clientid, TBL_BOOKING.companyid, TBL_BOOKING.status,
            TBL_BOOKING.staffid,  TBL_BOOKING.pos, TBL_PAYMENTINFO.paymenttype, TBL_AIRORDERS.productsubcategory, TBL_ACCOORDERS.productsubcategory,TBL_ACTIVITIESORDERS.productSubCategory,
			TBL_HOLIDAYSORDERS.productSubCategory
            order by TBL_BOOKING.createdat desc
			OFFSET p_startindex LIMIT p_limit;

       RETURN cur_Bookings;
                END;
$$ LANGUAGE plpgsql;
COMMIT;


-- Sample Test Examples
-- rollback;
--  BEGIN;
--     SELECT be_db_services_schema.SearchBookings(
--           0, 38, null,
--         	( SELECT CAST(ROW( null, 'Online', null , null, null,null, null, null, null) As be_db_services_schema.BookingSearchCriteria) ),
--          null,
--         	( SELECT CAST(ROW( null, null, null , null, null,null, null) As be_db_services_schema.ClientPaxSearchCriteria)),
--  	    	--Product Sub Category is Mandatory--
--  	    	( SELECT CAST(ROW('Flight', null , null, null ,null, null, null ,null ) As be_db_services_schema.FlightSearchCriteria ) ),
--         	( SELECT CAST(ROW('Hotel', null, null, null, null, null ) As be_db_services_schema.HotelSearchCriteria ) ),
--         	( SELECT CAST(ROW( 'Events', null, null, null, null, null ) As be_db_services_schema.activitiesSearchCriteria ) ),
--  			( SELECT CAST(ROW( 'Holidays', null, null, null, null, null,null, null,null, null, null, null ) As be_db_services_schema.HolidaysSearchCriteria ) )
--         	);
--
-- 	select * from be_db_services_schema.SearchBookings(0, 10, NULL, '(,,,,,,,,)', '(,,,,,)', '(,,,,ShivamS,,)',
-- 												   '(Flight,,,,,,,)', '(Hotel,,,,,)', '(Events,,,,,)', '(Holidays,,,,,,,,,,,)' )
--
--     FETCH ALL IN "<unnamed portal 2>" ;
--  COMMIT;




