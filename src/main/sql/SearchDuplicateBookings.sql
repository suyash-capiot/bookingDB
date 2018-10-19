-- FUNCTION: be_db_services_schema.searchduplicatebookings(character varying, be_db_services_schema.duplicateflightsearchcriteria[], be_db_services_schema.duplicateaccosearchcriteria[])

-- DROP FUNCTION be_db_services_schema.searchduplicatebookings(character varying, be_db_services_schema.duplicateflightsearchcriteria[], be_db_services_schema.duplicateaccosearchcriteria[]);

rollback;
DROP Function be_db_services_schema.searchduplicatebookings(p_bookId character varying,
	p_duplicateFlightSearchCriteria be_db_services_schema.duplicateFlightSearchCriteria[], p_duplicateaccosearchcriterialist be_db_services_schema.duplicateaccosearchcriteria[] )
    cascade;

DROP TYPE be_db_services_schema.duplicateFlightSearchCriteria;
DROP TYPE be_db_services_schema.duplicateaccosearchcriteria;

COMMIT

create type be_db_services_schema.duplicateFlightSearchCriteria AS (
	airlinename varchar, flightnumber varchar,
	firstname varchar, lastname varchar,
	fromsector varchar, tosector varchar,
	travelfromdate varchar, traveltodate varchar,
	cabinType varchar )


CREATE TYPE be_db_services_schema.duplicateaccosearchcriteria AS
(
	firstname character varying,
	lastname character varying,
	checkindate character varying,
	checkoutdate character varying,
	hotelcode character varying
);

CREATE OR REPLACE FUNCTION be_db_services_schema.searchduplicatebookings(p_bookId character varying,
	p_duplicateflightsearchcriterialist be_db_services_schema.duplicateflightsearchcriteria[],
	p_duplicateaccosearchcriterialist be_db_services_schema.duplicateaccosearchcriteria[])
    RETURNS SETOF text AS $BODY$

    DECLARE
        	p_duplicateFlightSearchCriteria be_db_services_schema.duplicateFlightSearchCriteria;
            p_duplicateAccoSearchCriteria be_db_services_schema.duplicateaccosearchcriteria;
            cur_BookingRow Record;
            titles TEXT DEFAULT '';

    		----Declare Cursors----
            cur_FlightBookings cursor(p_bookId character varying, p_duplicateFlightSearchCriteria be_db_services_schema.duplicateFlightSearchCriteria) FOR
                select
                TBL_BOOKING.bookid as bookId
                FROM
                be_db_services_schema.Booking as TBL_BOOKING
                LEFT JOIN be_db_services_schema.airorders TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.paxdetails) each_paxDetail_attribute ON TRUE
                LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS ON TBL_PASSENGERDETAILS.passanger_id = each_paxDetail_attribute ->>'paxID'
                LEFT JOIN LATERAL jsonb_array_elements(TBL_AIRORDERS.flightdetails->'originDestinationOptions') each_odo ON TRUE
                LEFT JOIN LATERAL jsonb_array_elements(each_odo -> 'flightSegment') each_flight_segment ON TRUE
                where
                        (
                            ( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                            and
                            (
                                (p_duplicateFlightSearchCriteria.flightNumber like  each_flight_segment -> 'operatingAirline' ->> 'flightNumber')
                            )
                            and
                            (
                                ( UPPER(p_duplicateFlightSearchCriteria.firstName) like UPPER(TBL_PASSENGERDETAILS.firstname) )
                            )
                            and
                            (
                                ( UPPER(p_duplicateFlightSearchCriteria.lastName) like UPPER(TBL_PASSENGERDETAILS.lastname))
                            )
                            -- Assumtion From Sector is Origin City --
                            and
                            (
                               ( p_duplicateFlightSearchCriteria.fromSector = each_flight_segment ->> 'originLocation')
                            )
                            and
                            (
                               ( p_duplicateFlightSearchCriteria.toSector like each_flight_segment ->> 'destinationLocation' )
                            )
                            and
                            (
                                ( date(p_duplicateFlightSearchCriteria.travelFromDate) = date(each_flight_segment ->>'departureDate'))
                            )
                            and
                            (
                                ( date(p_duplicateFlightSearchCriteria.travelToDate) = date(each_flight_segment ->>'arrivalDate'))
                            )
                            and
                            (    -- assumption cabin type is the class --
                                ( UPPER(p_duplicateFlightSearchCriteria.cabinType) like UPPER(each_flight_segment ->>'cabinType'))
                            )
                    	)
                group by TBL_BOOKING.bookid;
         -- End Cursors definition--

         cur_AccoBookings cursor(p_bookId character varying, p_duplicateAccoSearchCriteria be_db_services_schema.duplicateaccosearchcriteria) FOR
                select
                TBL_BOOKING.bookid as bookId
                FROM
                be_db_services_schema.Booking as TBL_BOOKING
                LEFT JOIN be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
                LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOROOMDETAILS ON TBL_ACCOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                LEFT JOIN LATERAL jsonb_array_elements(TBL_ACCOROOMDETAILS.paxdetails) each_AccoPaxDetail_attribute ON TRUE
                LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS ON TBL_PASSENGERDETAILS.passanger_id = each_AccoPaxDetail_attribute ->>'paxID'
        		where(
                            (
                                ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                            )
                            and
                            (
                                ( UPPER(p_duplicateAccoSearchCriteria.firstName) like UPPER(TBL_PASSENGERDETAILS.firstname) )
                            )
                            and
                            (
                                ( UPPER(p_duplicateAccoSearchCriteria.lastName) like UPPER(TBL_PASSENGERDETAILS.lastname))
                            )
                            and
                            (
                               ( date(p_duplicateAccoSearchCriteria.checkindate) = date(TBL_ACCOROOMDETAILS.checkindate) )
                            )
                            and
                            (
                               ( date(p_duplicateAccoSearchCriteria.checkoutdate) = date(TBL_ACCOROOMDETAILS.checkoutdate) )
                            )
                            and
                            (
                                (p_duplicateAccoSearchCriteria.hotelCode = TBL_ACCOROOMDETAILS.hotelCode )
                            )
                    	)
            group by TBL_BOOKING.bookid;



        BEGIN
           If p_duplicateFlightSearchCriteriaList  IS NOT NULL THEN
        		FOREACH p_duplicateFlightSearchCriteria IN ARRAY p_duplicateFlightSearchCriteriaList
                LOOP
                    OPEN cur_FlightBookings(p_bookId, p_duplicateFlightSearchCriteria);
                    --fetch cur_Bookings collect into cur_Booking_Rows;
                    LOOP
                        FETCH cur_FlightBookings INTO cur_BookingRow;
                            EXIT WHEN NOT FOUND;
                        RETURN NEXT cur_BookingRow.bookId;
                         --titles := titles || ',' || 'bookId: ' || cur_BookingRow.bookId || ', orderId: ' || cur_BookingRow.orderId ;
                    END LOOP;
                    CLOSE cur_FlightBookings;
               END LOOP;
           END IF;

           If p_duplicateaccosearchcriterialist IS NOT NULL THEN
        		FOREACH p_duplicateaccosearchcriteria IN ARRAY p_duplicateaccosearchcriterialist
                LOOP
                    OPEN cur_AccoBookings(p_bookId, p_duplicateaccosearchcriteria);
                    --fetch cur_Bookings collect into cur_Booking_Rows;
                    LOOP
                        FETCH cur_AccoBookings INTO cur_BookingRow;
                            EXIT WHEN NOT FOUND;
                        RETURN NEXT cur_BookingRow.bookId;
                         --titles := titles || ',' || 'bookId: ' || cur_BookingRow.bookId || ', orderId: ' || cur_BookingRow.orderId ;
                    END LOOP;
                    CLOSE cur_AccoBookings;
               END LOOP;
           END IF;


        END;
    $BODY$ LANGUAGE plpgsql;
    COMMIT;

rollback
-- Test stored proc using these queries below --
--BEGIN;
--   SELECT * FROM be_db_services_schema.SearchDuplicateBookings(null,
--      ARRAY[ROW(null,'122','PIYUSHA','NAIK','DEL','LHR','2018-07-25T13:05:00.000','2018-07-25T18:20:00.000','Economy')]
--       ::be_db_services_schema.duplicateFlightSearchCriteria[],
--   ARRAY[ROW(null,'ab','doe','2018-05-25','2018-05-28','76099')]::be_db_services_schema.duplicateaccosearchcriteria[])
--
--
--   SELECT * FROM be_db_services_schema.SearchDuplicateBookings(null,null,
--   ARRAY[ROW('ab','doe','2018-05-25','2018-05-28','76099')]::be_db_services_schema.duplicateaccosearchcriteria[] )
--
--
--    SELECT * FROM be_db_services_schema.SearchDuplicateBookings(null,
--      ARRAY[ROW(null, '338-Aavish', 'Raj', 'Surve', 'BOM', 'DEL', '2020-07-12T04:15:00', '2020-07-30T15:18:00',
--        'Economy')]::be_db_services_schema.duplicateFlightSearchCriteria[],null )
--
--
--    SELECT * FROM be_db_services_schema.SearchDuplicateBookings(null,
--      ARRAY[ROW(null, '338-Aavish', 'Raj', 'Surve', 'BOM', 'DEL', '2020-07-12T04:15:00', '2020-07-30T15:18:00',
--        'Economy'), ROW(null,'122','PIYUSHA','NAIK','DEL','LHR','2018-07-25T13:05:00.000','2018-07-25T18:20:00.000','Economy'),
--           ROW(null,'122','PIYUSHA','NAIK','DEL','LHR','2018-07-25T13:05:00.000','2018-07-25T18:20:00.000','Economy')]
--       ::be_db_services_schema.duplicateFlightSearchCriteria[],null )
--
--    SELECT * FROM be_db_services_schema.SearchDuplicateBookings( null, '338-Aavish', 'Raj', 'Surve', 'BOM', 'DEL', '2020-07-12T04:15:00', '2020-07-30T15:18:00',
--        'Economy')
--
--    SELECT * FROM be_db_services_schema.SearchDuplicateBookings(null,'122','PIYUSHA','NAIK','DEL','LHR','2018-07-25T13:05:00.000','2018-07-25T18:20:00.000','Economy')
--
--    select * from be_db_services_schema.SearchDuplicateBookings(
--'{"(, 154,PIYUSHA,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--"(, 154,PIYUSH,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--"(, 154,CHINTOO,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--"(, 154,PINTOO,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)"}'::be_db_services_schema.duplicateFlightSearchCriteria[] )  as result
--
--    select * from be_db_services_schema.SearchDuplicateBookings(NULL,
--    '{"(, 154,PIYUSHA,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--    "(, 154,PIYUSH,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--    "(, 154,CHINTOO,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--    "(, 154,PINTOO,NAIK,BOM,DEL,2018-07-30T01:50:00,2018-07-30T04:15:00,Economy)",
--    "(, 338,PIYUSHA,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,PIYUSH,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,CHINTOO,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,PINTOO,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(,338-Aavish,Raj,Surve,BOM,DEL,2020-07-12T04:15:00,2020-07-30T15:18:00,Economy)",
--    "(,338-Aavish,PIYUSH,NAIK,BOM,DEL,2020-07-12T04:15:00,2020-07-30T15:18:00,Economy)",
--    "(,338-Aavish,PINTOO,NAIK,BOM,DEL,2020-07-12T04:15:00,2020-07-30T15:18:00,Economy)",
--    "(,338-Aavish,CHINTOO,NAIK,BOM,DEL,2020-07-12T04:15:00,2020-07-30T15:18:00,Economy)",
--    "(, 338,PIYUSH,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,PINTOO,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,CHINTOO,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)",
--    "(, 338,PIYUSHA,NAIK,DEL,BOM,2018-07-25T10:45:00,2018-07-25T12:50:00,Economy)"}'::be_db_services_schema.duplicateFlightSearchCriteria[],
--    NULL::be_db_services_schema.duplicateaccosearchcriteria[])
--
--
--     FETCH ALL IN 123456;
--      FETCH ALL IN "<unnamed portal 2>";
--COMMIT;
--Search passeneger by id
---select * from be_db_services_schema.passengerdetails where passanger_id='8a9882786300d6eb0163015211c10000'--

