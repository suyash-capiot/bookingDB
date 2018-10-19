--- SearchBookings and Search failed bookings should be created and dropped at same time---
DROP FUNCTION be_db_services_schema.SearchFailedBookings(
    p_startIndex IN INTEGER,
    p_endIndex IN INTEGER,
    p_sortField IN VARCHAR,
    p_failureflag IN VARCHAR,
    p_BookingSearchCriteria IN be_db_services_schema.BookingSearchCriteria,
	p_CompaniesSearchCriteria IN be_db_services_schema.CompaniesSearchCriteria,
	p_ClientPaxSearchCriteria IN be_db_services_schema.ClientPaxSearchCriteria,
    p_FlightSearchCriteria IN be_db_services_schema.FlightSearchCriteria,
	p_HotelSearchCriteria IN be_db_services_schema.HotelSearchCriteria
) cascade;

CREATE OR REPLACE FUNCTION be_db_services_schema.searchfailedbookings(
	p_startindex integer,
	p_endindex integer,
	p_sortfield character varying,
	p_failureflag character varying,
	p_bookingsearchcriteria be_db_services_schema.bookingsearchcriteria,
	p_companiessearchcriteria be_db_services_schema.companiessearchcriteria,
	p_clientpaxsearchcriteria be_db_services_schema.clientpaxsearchcriteria,
	p_flightsearchcriteria be_db_services_schema.flightsearchcriteria,
	p_hotelsearchcriteria be_db_services_schema.hotelsearchcriteria)
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
            (
                select
                TBL_BOOKING.bookid,  TBL_BOOKING.createdat, each_booking_attribute as bookingAttribute,
                TBL_BOOKING.clientid, TBL_BOOKING.clienttype, TBL_BOOKING.pos as pointofsale, TBL_BOOKING.companyid, TBL_BOOKING.status,
                TBL_BOOKING.staffid, TBL_AIRORDERS.productsubcategory as productsubcategory, TBL_AIRORDERS.id as orderId, TBL_AIRORDERS.status as orderStatus,
			 	date(each_flight_segment ->>'arrivalDate') as travelDate,
			 	concat_ws(', ',each_flight_segment ->> 'originLocation', each_flight_segment ->> 'destinationLocation',
			 	each_flight_segment -> 'operatingAirline' ->> 'airlineCode', each_flight_segment  -> 'operatingAirline' ->> 'flightNumber',
			 	to_timestamp(each_flight_segment ->>'arrivalDate', '.YYYY.MM.DD HH24:MI:SS')::time ,
			 	to_timestamp(each_flight_segment ->> 'departureDate', 'YYYY.MM.DD HH24:MI:SS')::time ) as detailsSummary,
                TBL_AIRORDERS.supplierid as supplierid
                FROM
                be_db_services_schema.Booking as TBL_BOOKING
                LEFT JOIN be_db_services_schema.airorders as TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.bookingattribute) each_booking_attribute ON TRUE
                LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.flightdetails->'originDestinationOptions') each_odo ON TRUE
                LEFT JOIN LATERAL jsonb_array_elements(each_odo -> 'flightSegment') each_flight_segment ON TRUE
                where 1 = 1 and
 				(
                	( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                )
                and
                (
                     p_failureflag IS NULL OR each_booking_attribute @> p_failureflag::jsonb
				)
                and
                (	-- BEGIN Booking based Search Criteria
                    -- Search Param missing Booking Type, Prioroty, Assigmment Status, Financial Control Id
                    ( p_BookingSearchCriteria.BookingID IS NULL OR  TBL_BOOKING.bookid  = p_BookingSearchCriteria.BookingID )
                    and ( p_BookingSearchCriteria.BookingFromDate IS NULL OR date(TBL_BOOKING.createdat) >= date(p_BookingSearchCriteria.BookingFromDate))
                    and ( p_BookingSearchCriteria.BookingToDate IS NULL OR date(TBL_BOOKING.createdat) <= date(p_BookingSearchCriteria.BookingToDate))
                    and ( p_BookingSearchCriteria.BookingStatus IS NULL OR UPPER(TBL_BOOKING.status) like UPPER(p_BookingSearchCriteria.BookingStatus))
                    --and ( p_BookingSearchCriteria.priority IS NULL OR TBL_BOOKING.?? = p_BookingSearchCriteria.priority )
                    --and ( p_BookingSearchCriteria.AssignmentStatus IS NULL OR TBL_BOOKING.?? = p_BookingSearchCriteria.AssignmentStatus )
                    and ( p_BookingSearchCriteria.UserID IS NULL OR TBL_BOOKING.userid = p_BookingSearchCriteria.UserID )
                    --and ( p_BookingSearchCriteria.FinancialControlId IS NULL OR TBL_BOOKING.staffid = p_BookingSearchCriteria.FinancialControlId )
                ) -- END Booking based Search Criteria
                and
                (
                -- Search params!!
                -- AirlineName varchar, Supplier Reference Number / PNR
                    ( p_FlightSearchCriteria.ProductSubCategory IS NULL OR UPPER( TBL_AIRORDERS.productsubcategory) LIKE UPPER(p_FlightSearchCriteria.ProductSubCategory))
                    and ( p_FlightSearchCriteria.AirlinePNR IS NULL OR UPPER( TBL_AIRORDERS.airlinepnr) like UPPER( p_FlightSearchCriteria.AirlinePNR ) )
                    and ( p_FlightSearchCriteria.GdsPNR IS NULL OR UPPER( TBL_AIRORDERS.gdspnr ) like UPPER( p_FlightSearchCriteria.GdsPNR ) )
                    and ( p_FlightSearchCriteria.TravelFromDate IS NULL OR  ( date(each_flight_segment ->> 'departureDate')
                                                                                =  date( p_FlightSearchCriteria.TravelFromDate) ) )
                    and ( p_FlightSearchCriteria.TravelToDate IS NULL OR   ( date(each_flight_segment ->>'arrivalDate')
                                                                                = date( p_FlightSearchCriteria.TravelToDate) ) )
                    --Assumption TestTicketNumber is ticktetingpnr
                    and ( p_FlightSearchCriteria.TicketNumber IS NULL OR UPPER( TBL_AIRORDERS.ticketingpnr ) like UPPER( p_FlightSearchCriteria.TicketNumber ) )
                     and ( p_FlightSearchCriteria.SupplierName IS NULL OR UPPER(TBL_AIRORDERS.supplierId) like UPPER( p_FlightSearchCriteria.SupplierName ) )
                )
                and
                (	-- BEGIN Company Details Filter criteria
                    -- Check with Pritish on BE Table column Company Market, SBUId, BUId
                    ( p_CompaniesSearchCriteria.GroupOfCompaniesId IS NULL OR TBL_BOOKING.groupofcomapniesid >= p_CompaniesSearchCriteria.GroupOfCompaniesId)
                    and ( p_CompaniesSearchCriteria.GroupNameId IS NULL OR TBL_BOOKING.groupcompanyid = p_CompaniesSearchCriteria.GroupNameId )
                    and ( p_CompaniesSearchCriteria.CompanyId IS NULL OR TBL_BOOKING.companyid = p_CompaniesSearchCriteria.CompanyId )
                    --and ( p_CompaniesSearchCriteria.CompanyMarketId IS NULL OR TBL_BOOKING.createdat >= p_CompaniesSearchCriteria.CompanyMarketId)
                    and ( p_CompaniesSearchCriteria.SBUId IS NULL OR TBL_BOOKING.sbu = p_CompaniesSearchCriteria.SBUId)
                    and ( p_CompaniesSearchCriteria.BUId IS NULL OR TBL_BOOKING.bu >= p_CompaniesSearchCriteria.BUId)
                )	-- END Company Details Filter criteria
                and
                (
                    --ClientCategory varchar, Client SubCategory varchar, ClientName varchar, PassengerName varchar, PhoneNumber varchar
                    ( p_clientpaxsearchcriteria.ClientType IS NULL OR (TBL_BOOKING.clienttype ) LIKE UPPER(  p_clientpaxsearchcriteria.ClientType))
                     and (p_clientpaxsearchcriteria.ClientId IS NULL OR UPPER(TBL_BOOKING.clientId) LIKE UPPER (p_clientpaxsearchcriteria.ClientId)  )
                )
                --and TBL_ACCOOROOMDETAILS.acco_order_id =
                group by
                TBL_BOOKING.bookid, TBL_BOOKING.createdat,  each_booking_attribute,
                TBL_BOOKING.clientid, TBL_BOOKING.clienttype, TBL_BOOKING.pos, TBL_BOOKING.companyid, TBL_BOOKING.status,
                TBL_BOOKING.staffid, productsubcategory, orderId, orderStatus,
                travelDate, detailsSummary,
				supplierid
            )
            UNION ALL
            (
            	select
                TBL_BOOKING.bookid,  TBL_BOOKING.createdat, each_booking_attribute as bookingAttribute,
                TBL_BOOKING.clientid, TBL_BOOKING.clienttype, TBL_BOOKING.pos as pointofsale, TBL_BOOKING.companyid, TBL_BOOKING.status,
                TBL_BOOKING.staffid, TBL_ACCOORDERS.productsubcategory as productsubcategory, TBL_ACCOORDERS.id as orderId, TBL_ACCOORDERS.status as orderStatus,
                date(TBL_ACCOOROOMDETAILS.checkindate) as travelDate, concat_ws(', ', TBL_ACCOOROOMDETAILS.checkindate, TBL_ACCOOROOMDETAILS.checkoutdate) as detailsSummary,
                TBL_ACCOORDERS.supplierid as supplierid
                FROM
                be_db_services_schema.Booking as TBL_BOOKING
                LEFT JOIN  be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
				LEFT JOIN LATERAL jsonb_array_elements(TBL_ACCOORDERS.bookingattribute) each_booking_attribute ON TRUE
                LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOOROOMDETAILS ON TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                where 1 = 1 and
                (
                    ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                )
                and
                (
                    p_failureflag IS NULL OR each_booking_attribute @> p_failureflag::jsonb
				)
                and
                (	-- BEGIN Booking based Search Criteria
                    -- Search Param missing Booking Type, Prioroty, Assigmment Status, Financial Control Id
                    ( p_BookingSearchCriteria.BookingID IS NULL OR  TBL_BOOKING.bookid  = p_BookingSearchCriteria.BookingID )
                    and ( p_BookingSearchCriteria.BookingFromDate IS NULL OR date(TBL_BOOKING.createdat) >= date(p_BookingSearchCriteria.BookingFromDate))
                    and ( p_BookingSearchCriteria.BookingToDate IS NULL OR date(TBL_BOOKING.createdat) <= date(p_BookingSearchCriteria.BookingToDate))
                    and ( p_BookingSearchCriteria.BookingStatus IS NULL OR UPPER(TBL_BOOKING.status) like UPPER(p_BookingSearchCriteria.BookingStatus))
                    --and ( p_BookingSearchCriteria.priority IS NULL OR TBL_BOOKING.?? = p_BookingSearchCriteria.priority )
                    --and ( p_BookingSearchCriteria.AssignmentStatus IS NULL OR TBL_BOOKING.?? = p_BookingSearchCriteria.AssignmentStatus )
                    and ( p_BookingSearchCriteria.UserID IS NULL OR TBL_BOOKING.userid = p_BookingSearchCriteria.UserID )
                    --and ( p_BookingSearchCriteria.FinancialControlId IS NULL OR TBL_BOOKING.staffid = p_BookingSearchCriteria.FinancialControlId )
                ) -- END Booking based Search Criteria
                and
                (	-- BEGIN Hotel details Filter criteria
                    --  ProductName
                    ( p_HotelSearchCriteria.ProductSubCategory IS NULL OR UPPER (TBL_ACCOORDERS.productsubcategory) LIKE UPPER( p_HotelSearchCriteria.ProductSubCategory))
                    and ( p_HotelSearchCriteria.Country IS NULL OR UPPER(TBL_ACCOOROOMDETAILS.countrycode) LIKE UPPER( p_HotelSearchCriteria.Country ))
                    and ( p_HotelSearchCriteria.City IS NULL OR UPPER(TBL_ACCOOROOMDETAILS.citycode) LIKE UPPER( p_HotelSearchCriteria.City ))
                    and ( p_HotelSearchCriteria.SupplierName IS NULL OR UPPER(TBL_ACCOORDERS.supplierid) LIKE UPPER( p_HotelSearchCriteria.SupplierName ))
                    and ( p_HotelSearchCriteria.SupplierReferenceNumber IS NULL OR UPPER( TBL_ACCOORDERS.supplierreferenceid ) LIKE UPPER( p_HotelSearchCriteria.SupplierReferenceNumber ) )
                )
                and
                (	-- BEGIN Company Details Filter criteria
                    -- Check with Pritish on BE Table column Company Market, SBUId, BUId
                    ( p_CompaniesSearchCriteria.GroupOfCompaniesId IS NULL OR TBL_BOOKING.groupofcomapniesid >= p_CompaniesSearchCriteria.GroupOfCompaniesId)
                    and ( p_CompaniesSearchCriteria.GroupNameId IS NULL OR TBL_BOOKING.groupcompanyid = p_CompaniesSearchCriteria.GroupNameId )
                    and ( p_CompaniesSearchCriteria.CompanyId IS NULL OR TBL_BOOKING.companyid = p_CompaniesSearchCriteria.CompanyId )
                    --and ( p_CompaniesSearchCriteria.CompanyMarketId IS NULL OR TBL_BOOKING.createdat >= p_CompaniesSearchCriteria.CompanyMarketId)
                    and ( p_CompaniesSearchCriteria.SBUId IS NULL OR TBL_BOOKING.sbu LIKE UPPER(p_CompaniesSearchCriteria.SBUId))
                    and ( p_CompaniesSearchCriteria.BUId IS NULL OR TBL_BOOKING.bu LIKE UPPER(p_CompaniesSearchCriteria.BUId))
                )	-- END Company Details Filter criteria
                and
                (
                    --ClientCategory varchar, Client SubCategory varchar, ClientName varchar, PassengerName varchar, PhoneNumber varchar
                    ( p_clientpaxsearchcriteria.ClientType IS NULL OR (TBL_BOOKING.clienttype ) LIKE UPPER(  p_clientpaxsearchcriteria.ClientType))
                     and (p_clientpaxsearchcriteria.ClientId IS NULL OR UPPER(TBL_BOOKING.clientId) LIKE UPPER (p_clientpaxsearchcriteria.ClientId)  )
                )
                --and TBL_ACCOOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                group by
                TBL_BOOKING.bookid, TBL_BOOKING.createdat,  each_booking_attribute,
                TBL_BOOKING.clientid, TBL_BOOKING.clienttype, TBL_BOOKING.pos, TBL_BOOKING.companyid, TBL_BOOKING.status,
                TBL_BOOKING.staffid, productsubcategory, orderId, orderStatus,
                travelDate, detailsSummary,
				supplierid
            )
            order by bookId desc;

      RETURN cur_Bookings;
                END;
$$ LANGUAGE plpgsql;
COMMIT;


-- rollback;
-- BEGIN;
--    SELECT be_db_services_schema.SearchFailedBookings(
--          0, 0, null,'{"BF":"Failure Booking"}',
--        	( SELECT CAST(ROW( 'test123', null, null , null, null,null, null, null, null) As be_db_services_schema.BookingSearchCriteria) ),
--         null,
--        	( SELECT CAST(ROW( null, null, null , null, null,null, null) As be_db_services_schema.ClientPaxSearchCriteria)),
--        	( SELECT CAST(ROW( null, null , null, null,null, null, null ,null ) As be_db_services_schema.FlightSearchCriteria ) ),
--        	( SELECT CAST(ROW( null, null, null, null, null, null ) As be_db_services_schema.HotelSearchCriteria ) )
--        	);

--    FETCH ALL IN "<unnamed portal 3>" ;
-- END
--/*
--Output variables:
--BookingID
--Booking Date time
--Client details (client name)
--Client type
--Point of Sale
--Product summary (list of all products)
--Booking status
--Company Details
--Payment status
--Assigned to
--*/




