rollback;
CREATE OR REPLACE FUNCTION be_db_services_schema.searcharrivallisthotel(
	p_checkInDate character varying,
	p_bookingdatetime character varying,
	p_productsubcategory character varying,
	p_supplierid character varying,
	p_clienttype character varying,
	p_clientgroupid character varying,
	p_clientid character varying,
    p_continent character varying, --Hotel Continet not availiable in address--
	p_country character varying, --Hotel country not avaliable in AccoOrders.address---
	p_city character varying, --Hotel city not avaliable in AccoOrders.address---
	p_productName character varying,
	p_chain character varying,
	p_isMsteryProduct character varying)
    RETURNS refcursor AS $$

    DECLARE
		p_BookingTimeStamp timestamp:= null;
        p_checkInDateTimeStamp timestamp:= null;

        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN

                OPEN cur_Bookings FOR
                (
 						--Assumption Supplier Ref Numer is Supplier Ref Id--
                        select
                        TBL_BOOKING.bookid as bookid, TBL_ACCOORDERS.supplierreferenceid as supplier_reference_number, 'Accomodation' as productcategory,
                        TBL_ACCOORDERS.productsubcategory as productsubcategory, TBL_ACCOORDERS.supplierid as supplierid,
    					TBL_ACCOORDERS.supplierreferenceid as supplierreferenceid,TBL_ACCOROOMDETAILS.checkindate as checkInDate, TBL_ACCOROOMDETAILS.checkoutdate as checkOutDate,
                        TBL_ACCOROOMDETAILS.roomcategory as roomCategory, TBL_ACCOROOMDETAILS.roomtype as roomType,
    					jsonb_array_length(TBL_ACCOROOMDETAILS.paxdetails) as paxcount, TBL_PASSENGERDETAILS.firstname as firstName,
    					TBL_PASSENGERDETAILS.lastname as lastName, TBL_PASSENGERDETAILS.paxtype as paxtype,
                    	sum(case when TBL_ACCOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id then 1 else 0 end) as totalRoomsInOrder
    					FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN  be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
    					LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOROOMDETAILS ON TBL_ACCOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                        CROSS JOIN jsonb_array_elements(TBL_ACCOROOMDETAILS.paxdetails) each_paxDetail_attribute
                        LEFT JOIN be_db_services_schema.passengerdetails as TBL_PASSENGERDETAILS
                        ON TBL_PASSENGERDETAILS.passanger_id = each_paxDetail_attribute ->>'paxID'
    					where 1 = 1 and
                        (
                            ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                        )
                     	and
                            (
                               ( p_bookingdatetime IS NULL OR date(TBL_BOOKING.createdat) = date(p_bookingdatetime)	)
                            )
                        and
                            (
                               ( p_checkInDate IS NULL OR date(TBL_ACCOROOMDETAILS.checkindate) = date(p_checkInDate))
                            )
    					and
                        (
                            ( p_SupplierID IS NULL OR p_SupplierID like TBL_ACCOORDERS.supplierid )
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
                        group by TBL_BOOKING.bookid, supplier_reference_number, productsubcategory,
                        TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, supplierid, supplierreferenceid, checkInDate, checkOutDate,
                        roomCategory, roomType, paxcount, firstName, lastName, paxtype
                   order by bookid, productsubcategory desc
                );
               RETURN cur_Bookings;
            -- END OF IF CLAUSE --

      	END; -- END OF Function BEGIN tag

$$ LANGUAGE plpgsql;
COMMIT;

--rollback;

BEGIN;
   SELECT be_db_services_schema.searcharrivallisthotel( '2018-05-27' , '2018-04-23' ,null, 'BONOTEL', null,
                                                        null, null, null, null, null, null, null, null)
   FETCH ALL IN "<unnamed portal 4>" ;
COMMIT;

--  p_checkInDate character varying,
--	p_bookingdatetime character varying,
--	p_productsubcategory character varying,
--	p_supplierid character varying,
--	p_clienttype character varying,
--	p_clientgroupid character varying,
--	p_clientid character varying,
--    p_continent character varying, --Hotel Continet not availiable in address--
--	p_country character varying, --Hotel country not avaliable in AccoOrders.address---
--	p_city character varying, --Hotel city not avaliable in AccoOrders.address---
--	p_productName character varying,
--	p_chain character varying,
--	p_isMsteryProduct character varying










