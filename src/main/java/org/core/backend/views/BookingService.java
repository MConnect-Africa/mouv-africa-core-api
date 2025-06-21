package  org.core.backend.views;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.core.backend.models.Collections;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;
import org.core.backend.models.Status;


/**
 * The authentication service.
 */
public class BookingService extends ListingsService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        BookingService.class.getName());

    /** the double value 0.0. */
    private static final double ZERO_DOUBLE = 0.0;

    /**
     * Sets routes for the http server.
     * @param router The router used to set paths.
     */
    protected void setBookingServiceRoutes(final Router router) {
        this.logger.info(
            "setBookingServiceRoutes -> ()");

        router.post("/searchForValidBookingWindow")
            .handler(this::searchForValidBookingWindow);
        router.post("/makeABooking")
            .handler(this::makeABooking);
        router.post("/listBookings")
            .handler(this::listBookings);


        this.setListingsRoutes(router);
    }

    /**
     * Searches for a valid booking spot.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "searchForValidBookingWindow")
    private void searchForValidBookingWindow(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchForValidBookingWindow", rc,
            (xusr, body, params, headers, resp) -> {

                // Apply role-based query filters
                this.getUtils().assignRoleQueryFilters(
                    xusr, body, false);

                // Add search functionality for custom fields
                this.getUtils().addFieldsToSearchQuery(body);

                this.getDbUtils().find(
                    Collections.BOOKINGS.toString(),
                        body, resp);
        });
    }

    /**
     * makes a booking for a user.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "makeABooking")
    private void makeABooking(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "makeABooking", rc,
            (xusr, body, params, headers, resp) -> {

                // The start date should always be greater than all end dates
                // the start date should not be less than Today.

                try {
                    Date endDate = new Date(body.getLong("endDate"));
                    Date startDate = new Date(body.getLong("startDate"));

                    if (this.getUtils().compareDateToToday(startDate)
                        && this.getUtils().compareDateToToday(endDate)
                        && this.getUtils().compare(startDate, endDate)) {

                        JsonObject query = new JsonObject()
                            .put("endDate", new JsonObject()
                                .put("$lte", startDate.getTime()))
                            .put("status", Status.ACTIVE.name());

                        this.getDbUtils().findOne(
                            Collections.BOOKINGS.toString(), query, res -> {

                                if (res == null || res.isEmpty()) {
                                    // No existing booking.
                                    this.makeABooking(xusr, body, resp);
                                } else {
                                    resp.end(this.getUtils().getResponse(
                                        Utils.ERR_504,
                                        "This unit already has a booking")
                                    .encode());
                                }
                        }, resp);
                    } else {
                        resp.end(this.getUtils().getResponse(
                            Utils.ERR_503,
                        "Please pass valid start and end dates").encode());
                    }
                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }
        }, "startDate", "endDate", "listingId");
    }

    /**
     * Converts the long timestamp to a yyy-MM-dd format.
     * @param timestamp The timestamp
     * @return The date in format yyy-MM-dd
     */
    public String convertTimestampToDate(final long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getDefault());

        return sdf.format(new Date(timestamp));
    }

    /**
     * Makes a booking for a customer.
     * @param xusr The user object
     * @param body The body from FE
     * @param resp The server response.
     */
    protected void makeABooking(final JsonObject xusr,
        final JsonObject body, final HttpServerResponse resp) {

            try {

                long startDate = body.getLong("startDate");
                long endDate = body.getLong("endDate");

                JsonObject booking = new JsonObject()
                    .put("numberOfDays", this.getUtils()
                        .diffInDays(this.convertTimestampToDate(startDate),
                        this.convertTimestampToDate(endDate))
                        + Utils.M_1)
                    .put("startDate", startDate).put("endDate", endDate)
                    .put("listingId", body.getString("listingId"))
                    .put("status", Status.PENDING.toString())
                    .put("amenities",
                        body.getJsonArray("amenities", new JsonArray()));

                this.makeABooking(xusr, booking, body, resp);
            } catch (final Exception e) {
                this.logger.error(e.getMessage(), e);
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            }
    }

    /**
     * Makes a booking for a customer.
     * @param xusr The user object
     * @param booking The booking object we want to save.
     * @param body The body from FE
     * @param resp The server response.
     */
    protected void makeABooking(final JsonObject xusr, final JsonObject booking,
        final JsonObject body, final HttpServerResponse resp) {

            try {
                JsonObject qry = new JsonObject()
                    .put("_id", body.getString("listingId"));

                this.getDbUtils().findOne(Collections.LISTINGS.toString(),
                    qry, res -> {

                        if (res == null || res.isEmpty()) {
                            resp.end(this.getUtils().getResponse(
                                Utils.ERR_505,
                            "Unit passed does not exist").encode());
                        } else {
                            this.makeABooking(
                                xusr, booking, res, body, resp);
                        }

                    }, resp);

            } catch (final Exception e) {
                this.logger.error(e.getMessage(), e);
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            }

    }


    /**
     * Makes a booking for a customer.
     * @param listing The unit being booked.
     * @param xusr The user object.
     * @param booking The booking object we want to save.
     * @param body The body from FE.
     * @param resp The server response.
     */
    protected void makeABooking(final JsonObject xusr,
        final JsonObject booking, final JsonObject listing,
        final JsonObject body, final HttpServerResponse resp) {

            try {
                if (this.getUtils().isValid(xusr)
                    && this.getUtils().isValid(booking)
                    && this.getUtils().isValid(listing)
                    && this.getUtils().isValid(body)) {

                    this.getUtils().assignRoleSaveFilters(listing, booking);
                    booking
                        .put("feduid", xusr.getString("feduid"))
                        .put("clientId", xusr.getString("_id"))
                        .put("receipt", this.createReceiptForTransaction(
                            xusr, booking, listing, body));

                    this.getDbUtils().save(Collections.BOOKINGS.toString(),
                        booking, null, () -> {
                            resp.end(this.getUtils().getResponse(
                                booking).encode());
                            // send new to alert pending booking.
                    }, fail -> {
                        resp.end(this.getUtils().getResponse(
                            Utils.ERR_506, fail.getMessage()).encode());
                    });

                } else {
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_506, "Please pass valid params").encode());
                }
            } catch (final Exception e) {
                this.logger.error(e.getMessage(), e);
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            }

    }

    /**
     * Creates the object for the receipt.
     * @param xusr the usr object
     * @param booking The booking object
     * @param listing The listing object
     * @param body The body from the FE
     * @return create receipt
     */
    private JsonObject createReceiptForTransaction(final JsonObject xusr,
        final JsonObject booking, final JsonObject listing,
        final JsonObject body) {
        this.logger.info("createReceiptForTransaction -> ()");
        // the basic premium for nights slept
        // the payable amenities used.
        // VAT
        // Loading amount

        JsonObject premium = listing.getJsonObject(
            "premium", new JsonObject());
        System.out.println(premium.encode());
        double basicPremium = premium.getDouble("basicPremium",
            Utils.ZERO_DOUBLE);

        double totalStatutoryPremiums = this.calculateTaxes(
            premium.getJsonArray("statutoryPremiums"), ZERO_DOUBLE);
        double totalLoadingAmounts = this.calculateTaxes(
            premium.getJsonArray("loadings"), ZERO_DOUBLE);
        double totalDiscountsAmounts = this.calculateTaxes(
            premium.getJsonArray("discounts"), ZERO_DOUBLE);
        double totalAmenitiesAmounts = this.calculateTaxes(
            premium.getJsonArray("amenities"), ZERO_DOUBLE);

        double amount = basicPremium
            + totalStatutoryPremiums
            + totalLoadingAmounts
            - totalDiscountsAmounts;

        return new JsonObject()
            .put("basicPremium", basicPremium)
            .put("totalStatutoryPremiums", totalStatutoryPremiums)
            .put("totalLoadingAmounts", totalLoadingAmounts)
            .put("totalDiscountsAmounts", totalDiscountsAmounts)
            .put("totalAmenitiesAmounts", totalAmenitiesAmounts)
            .put("amount", amount);
    }

    /**
     * Calculates the taxes being imposed.
     * @param taxes The taxes to calculate.
     * @param basicPremium The basic premium.
     * @return taxes The result of taxes.
     */
    private double calculateTaxes(final JsonArray taxes,
        final double basicPremium) {
        if (taxes != null) {

            double result = Utils.ZERO;
            for (int i = 0; i < taxes.size(); i++) {
                JsonObject tax = taxes.getJsonObject(i);

                if (tax != null && !tax.isEmpty()) {
                    boolean isAmount = tax.getBoolean("isAmount");
                    double amount = tax.getDouble("amount", ZERO_DOUBLE);
                    if (isAmount) {
                        result += amount;
                        tax.put("value", amount);
                    } else {
                        result += (amount * basicPremium);
                        tax.put("value", ((amount * basicPremium) / HUNDRED));
                    }
                }
            }
            return result;
        }
        return ZERO_DOUBLE;
    }

    /**
     * Lists the bookings.
     * @param rc The routing context.
     */
    protected void listBookings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listBookings", rc,
            (xusr, body, params, headers, resp) -> {

                // Apply role-based query filters
                this.getUtils().assignRoleQueryFilters(
                    xusr, body, false);

                if (this.getUtils().isRole("client", xusr)) {
                    body.put("feduid", xusr.getString("feduid"));
                }
                // Add search functionality for custom fields
                this.getUtils().addFieldsToSearchQuery(body);

                this.getDbUtils().find(
                    Collections.BOOKINGS.toString(),
                        body, resp);
        });
    }
}
