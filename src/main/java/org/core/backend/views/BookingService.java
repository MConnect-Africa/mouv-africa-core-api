package  org.core.backend.views;


import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
                            .put("listingId", body.getString("listingId"))
                            .put("endDate", new JsonObject()
                                .put("$lte", startDate.getTime()))
                            .put("status", Status.ACTIVE.name());
                        System.out.println(query.encode());
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

                    body.fieldNames().forEach(key -> {
                        if (!key.equalsIgnoreCase("listingId")
                        && !key.equalsIgnoreCase("organisationId")
                        && !key.equalsIgnoreCase("feduid")
                        && !key.equalsIgnoreCase("client")
                        && !key.equalsIgnoreCase("numberOfDays")
                        && !key.equalsIgnoreCase("clientId")) {
                            booking.put(key, body.getValue(key));
                        }
                    });

                this.makeABooking(xusr, booking, body, resp);
            } catch (final Exception e) {
                this.logger.error(e.getMessage(), e);
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            }
    }

    /**
     * Apply dicounts if they are required.
     * @param body The body object
     * @param booking The booking object
     * @param listing The listing object
     * @return The list of applicable discounts
     */
    private JsonArray applyDiscountsIfNeedBe(final JsonObject body,
        final JsonObject booking, final JsonObject listing) {

            JsonArray allDiscounts = listing.getJsonArray(
                    "discounts", new JsonArray());

            JsonArray result = this.applicableDisounts(allDiscounts, booking);
            return result == null
                ? new JsonArray()
                : result;
    }

    /**
     * Checks for all applicable discounts possibly there is.
     * @param discounts The discounts arrays.
     * @param booking The booking object
     * @return A list of applicable discounts.
     */
    private JsonArray applicableDisounts(final JsonArray discounts,
        final JsonObject booking) {

        JsonArray results = new JsonArray();
        if (discounts != null && !discounts.isEmpty()) {

            for (int i = Utils.ZERO; i < discounts.size(); i++) {
                JsonObject discount = discounts.getJsonObject(i);
                if (discount != null && !discount.isEmpty()) {
                    if (discount.getBoolean("isWeekendOnly")
                        && this.verifyWeekendDiscount(discount)) {
                            results.add(discount);
                    }

                    if (this.verifyConsecutiveDaysDiscount(discount,
                        booking.getInteger("numberOfDays", Utils.ZERO))) {
                            results.add(discount);
                    }


                }
            }
        }
        return results;
    }

    /**
     * checks if we are eligible for a weekend price.
     * @param discount The discount object
     * @return if the discount is viable or not
     */
    private boolean verifyWeekendDiscount(final JsonObject discount) {
        if (discount != null && !discount.isEmpty()) {
             DayOfWeek day = LocalDate.now().getDayOfWeek();
            return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        }
        return false;
    }

    /**
     * checks if we are eligible for a weekend price.
     * @param discount The discount object
     * @param numberOfDays The number of days passed
     * @return if the discount is viable or not
     */
    private boolean verifyConsecutiveDaysDiscount(final JsonObject discount,
        final int numberOfDays) {
        if (discount != null && !discount.isEmpty()) {
            if (discount.containsKey("days")
                && discount.getInteger("days") != null) {
                    int expectedDays = discount.getInteger("days");
                    return expectedDays <= numberOfDays;
                }
        }
        return false;
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
                    .put("_id", body.getString("listingId"))
                    .put("status", Status.ACTIVE.name());

                this.getDbUtils().findOne(Collections.LISTINGS.toString(),
                    qry, res -> {

                        if (res == null || res.isEmpty()) {
                            resp.end(this.getUtils().getResponse(
                                Utils.ERR_505,
                            "Active Unit passed does not exist").encode());
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

        JsonArray appliedDiscounts =
            this.applyDiscountsIfNeedBe(body, booking, listing);
        JsonArray appliedAmenities
            = body.getJsonArray("amenities", new JsonArray());
        JsonObject premium = listing.getJsonObject(
            "premium", new JsonObject());
        int numberOfDays = booking.getInteger("numberOfDays");

        JsonArray statPremiums = premium.getJsonArray("statutoryPremiums");
        JsonArray loadingAmounts = premium.getJsonArray("loadings");
        double basicPremium = premium.getDouble(
            "basicPremium", Utils.ZERO_DOUBLE);

        double totalStatutoryPremiums = this.calculateTaxes(
            statPremiums, basicPremium, numberOfDays);
        double totalLoadingAmounts = this.calculateTaxes(
            loadingAmounts, basicPremium, numberOfDays);
        double totalDiscountsAmounts = this.calculateTaxes(
            appliedDiscounts, basicPremium, numberOfDays);
        double totalAmenitiesAmounts = this.calculateTaxes(
            appliedAmenities, basicPremium, numberOfDays);

        double amount = basicPremium
            + totalStatutoryPremiums
            + totalLoadingAmounts
            - totalDiscountsAmounts;

        return new JsonObject()
            .put("basicPremium", basicPremium * numberOfDays)
            .put("totalStatutoryPremiums", totalStatutoryPremiums)
            .put("totalLoadingAmounts", totalLoadingAmounts)
            .put("totalDiscountsAmounts", totalDiscountsAmounts)
            .put("appliedAmenities", appliedAmenities)
            .put("discounts", appliedDiscounts)
            .put("loadingAmounts", loadingAmounts)
            .put("statutoryPremiums", totalStatutoryPremiums)
            .put("totalAmenitiesAmounts", totalAmenitiesAmounts)
            .put("amount", amount);
    }

    /**
     * Calculates the taxes being imposed.
     * @param taxes The taxes to calculate.
     * @param basicPremium The basic premium.
     * @param numberOfDays The number of days.
     * @return taxes The result of taxes.
     */
    private double calculateTaxes(final JsonArray taxes,
        final double basicPremium, final double numberOfDays) {
        if (taxes != null) {

            double result = Utils.ZERO;
            for (int i = 0; i < taxes.size(); i++) {
                JsonObject tax = taxes.getJsonObject(i);

                if (tax != null && !tax.isEmpty()) {
                    boolean isAmount = tax.getBoolean("isAmount");
                    boolean isPaidDaily
                        = tax.getBoolean("isPaidDaily", false);
                    double amount = tax.getDouble("amount", ZERO_DOUBLE);
                    double resAmount = ZERO_DOUBLE;
                    if (isAmount) {
                        resAmount += amount;
                    } else {
                        resAmount += ((amount * basicPremium) / HUNDRED);
                    }

                    if (isPaidDaily) {
                        resAmount = numberOfDays * resAmount;
                    }

                    tax.put("value", resAmount);
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

                this.getDbUtils().aggregate(Collections.BOOKINGS.toString(),
                    this.createQueryForListings(xusr, body), resp);
                // this.getDbUtils().aggregate(
                //     Collections.BOOKINGS.toString(),
                //         body, resp);
        });
    }

    /**
     * Creates the aggregate query for listing.
     * @param xusr The user object
     * @param body The body by the FE
     * @return pipeline for the query sent
     */
    private JsonArray createQueryForListings(final JsonObject xusr,
        final JsonObject body) {
        this.logger.info("createQueryForListings -> ()");
        // Apply role-based query filters
        this.getUtils().assignRoleQueryFilters(
            xusr, body, false);

        if (this.getUtils().isRole("client", xusr)) {
            body.put("feduid", xusr.getString("feduid"));
        }
        // Add search functionality for custom fields
        this.getUtils().addFieldsToSearchQuery(body);

        JsonObject lookup = new JsonObject()
            .put("from", Collections.LISTINGS.toString())
            .put("localField", "listingId")
            .put("foreignField", "_id")
            .put("as", "listing");

        JsonObject listingTypeLookUp = new JsonObject()
            .put("from", Collections.LISTING_TYPES.toString())
            .put("localField", "listing.listingType")
            .put("foreignField", "_id")
            .put("as", "listingType");

        JsonObject clientLookUp = new JsonObject()
            .put("from", Collections.USERS.toString())
            .put("localField", "feduid")
            .put("foreignField", "feduid")
            .put("as", "client");

        return new JsonArray()
            .add(new JsonObject()
                .put("$match", body))
            .add(new JsonObject()
                .put("$lookup", lookup))
            .add(new JsonObject()
                .put("$unwind", "$listing"))
            .add(new JsonObject()
                .put("$lookup", listingTypeLookUp))
            .add(new JsonObject()
                .put("$unwind", "$listingType"))
            .add(new JsonObject()
                .put("$lookup", clientLookUp))
            .add(new JsonObject()
                .put("$unwind", "$client"));

    }
}
