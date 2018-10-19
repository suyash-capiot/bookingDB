CREATE OR REPLACE FUNCTION be_db_services_schema.searcharrivallistflight(
	p_traveldatetime character varying,
	p_bookingdatetime character varying,
	p_supplierid character varying,
	p_clienttype character varying,
	p_clientgroupid character varying,
	p_clientid character varying,
	p_fromcity character varying,
	p_tocity character varying,
	p_journeytype character varying)
    RETURNS refcursor AS $$


    DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
        	-- BEGIN IF CHECK FOR TRAVEL DATE ---
                OPEN cur_Bookings FOR
                    (
                        select
                        TBL_BOOKING.bookid as bookid, TBL_AIRORDERS.airlinepnr as supplier_reference_number,
                        'Transportation' as productcategory,
                        TBL_AIRORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
                        TBL_BOOKING.groupnameid as groupnameid, TBL_AIRORDERS.supplierid as supplierid,
                        each_flight_segment -> 'operatingAirline' ->> 'flightNumber' as flightnumber,
                        each_flight_segment -> 'operatingAirline' ->> 'airlineCode' as airlinecode,
                        each_flight_segment ->> 'originLocation' as originLocation,
                        each_flight_segment ->> 'destinationLocation' as destinationLocation,
                        TBL_PASSENGERDETAILS.firstname as firstName,  TBL_PASSENGERDETAILS.lastname as lastName,
                        TBL_PASSENGERDETAILS.paxtype as paxtype,
                        TBL_AIRORDERS.ticketingpcc,
                        each_flight_segment ->> 'cabinType' as cabinType,
                        each_flight_segment ->> 'rph' as rph,
                        jsonb_array_length( TBL_AIRORDERS.paxdetails ) as paxcount
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN be_db_services_schema.airorders TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                        CROSS JOIN jsonb_array_elements(TBL_AIRORDERS.paxdetails) each_paxDetail_attribute
                        LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS ON TBL_PASSENGERDETAILS.passanger_id = each_paxDetail_attribute ->>'paxID'
						CROSS JOIN jsonb_array_elements( TBL_AIRORDERS.flightdetails ->'originDestinationOptions'-> 0 ->'flightSegment' ) each_flight_segment
                        where 1 = 1 and
                        (
                            ( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                            and
                            (
                               ( p_BookingDateTime IS NULL OR date(TBL_BOOKING.createdat) = date(p_BookingDateTime)	)
                            )
                            and
                            (
                               ( p_TravelDateTime IS NULL OR TO_TIMESTAMP( p_TravelDateTime, 'YYYY-MM-DD HH24:MI:SS')
                                                                          = TO_TIMESTAMP( each_flight_segment ->>'departureDate', 'YYYY-MM-DD HH24:MI:SS' ))
                            )
                            and
                            (
                                ( p_SupplierID IS NULL OR p_SupplierID like TBL_AIRORDERS.supplierid )
                            )
                            and
                            (
                                ( p_ClientType IS NULL OR p_ClientType like TBL_BOOKING.clienttype )
                            )
                            and
                            (
                                ( p_ClientGroupID IS NULL OR p_ClientGroupID like TBL_BOOKING.groupnameid )
                            )
                            and
                            (
                                ( p_ClientID IS NULL OR p_ClientID like TBL_BOOKING.clientid )
                            )
                        )
                        group by
                        TBL_BOOKING.bookid, TBL_AIRORDERS.productsubcategory, TBL_AIRORDERS.airlinepnr,
                        TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, TBL_AIRORDERS.supplierid,
                        flightnumber, airlinecode, originLocation, destinationLocation, firstName, lastName,
                        TBL_PASSENGERDETAILS.paxtype, TBL_AIRORDERS.ticketingpcc, cabinType,
                        each_flight_segment ->> 'rph', paxcount
                        order by TBL_BOOKING.bookid, TBL_BOOKING.createdat desc
                    );

               RETURN cur_Bookings;
            END; -- END OF Function BEGIN tag
		 $$ LANGUAGE plpgsql;
    COMMIT;

rollback
-- Test stored proc using these queries below --
BEGIN;
   SELECT be_db_services_schema.SearchArrivalListFlight( null, null , 'INDIGO', null, null, null, null, null, null )
   FETCH ALL IN "<unnamed portal 2>" ;
COMMIT;
--'2018-04-26'
--'2018-07-30T01:50:00'
--TO_TIMESTAMP(TBL_BOOKING.createdat,'YYYY-MM-DD HH24:MI:SS')
--TO_TIMESTAMP( each_flight_segment ->>'departureDate', 'YYYY-MM-DD HH24:MI:SS' )
-- End Test queries --