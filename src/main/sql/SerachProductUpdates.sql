-- SQL Function for retrieving Product Update Search for Flights --
rollback;
CREATE OR REPLACE FUNCTION be_db_services_schema.SearchProductUpdates(
	p_startindex integer,
	p_limit integer,
	p_airlineName character varying, --talk to pritish--
	p_fromSector character varying,
	p_toSector character varying,
	p_flightNumber character varying,
	p_flightTimingFrom character varying,
	p_flightTimingTo character varying,
	p_supplierId character varying)
    RETURNS refcursor AS $$


    DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
        	-- BEGIN IF CHECK FOR TRAVEL DATE ---
                OPEN cur_Bookings FOR
                        select
                        TBL_BOOKING.bookid as bookId, TBL_AIRORDERS.id as orderId,TBL_AIRORDERS.supplierid as supplierid,
                        TBL_PASSENGERDETAILS.firstname as firstName, TBL_PASSENGERDETAILS.lastname as lastName,
                        each_flight_segment ->> 'originLocation' as fromSector,
                        each_flight_segment ->> 'destinationLocation' as toSector,
                        each_flight_segment -> 'operatingAirline' ->> 'flightNumber' as flightnumber,
                        date(each_flight_segment ->>'departureDate') as departureDate,
                        to_timestamp(each_flight_segment ->>'departureDate', 'YYYY.MM.DD HH24:MI:SS')::time as departureTime,
                        date(each_flight_segment ->> 'arrivalDate') as arrivalDate,
                        to_timestamp(each_flight_segment ->> 'arrivalDate', 'YYYY.MM.DD HH24:MI:SS')::time as arrivalTime,
						count(*) OVER() AS fullcount
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
                               ( p_fromSector IS NULL OR p_fromSector = each_flight_segment ->> 'originLocation')
                            )
                            and
                            (
                               ( p_toSector IS NULL OR p_toSector like each_flight_segment ->> 'destinationLocation' )
                            )
                            and
                            (
                                ( p_flightNumber IS NULL OR p_flightNumber like  each_flight_segment -> 'operatingAirline' ->> 'flightNumber')
                            )
                            and
                            (
                           		(TBL_PASSENGERDETAILS.isleadpax = TRUE)
                            )
                            and
                            (
                                (p_flightTimingFrom IS NULL OR  date(p_flightTimingFrom)
                                                                          <= date( each_flight_segment ->>'departureDate'))
                            )
                            and
                            (
                                (p_flightTimingTo IS NULL OR date(p_flightTimingTo)
                                                                          >= date( each_flight_segment ->>'arrivalDate'))
                            )
                            and
                            (
                                ( p_SupplierID IS NULL OR p_SupplierID like TBL_AIRORDERS.supplierid )
                            )
                        )
                        group by
                        TBL_BOOKING.bookid, orderId, supplierId,
                        firstName, lastName, fromSector, toSector,
                        flightnumber, departureDate, departureTime, arrivalDate, arrivalTime
                        order by TBL_BOOKING.bookid, TBL_BOOKING.createdat desc
						OFFSET p_startindex LIMIT p_limit;

               RETURN cur_Bookings;
            END; -- END OF Function BEGIN tag
    $$ LANGUAGE plpgsql;
    COMMIT;

rollback
-- Test stored proc using these queries below --
BEGIN;
   SELECT be_db_services_schema.SearchProductUpdates( 0, 20,null, null, null, null, '2017-04-02T06:00:00', '2019-04-02T06:00:00', null)
   FETCH ALL IN "<unnamed portal 1>" ;
COMMIT;