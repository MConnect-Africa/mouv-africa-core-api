package org.core.backend.views;

import io.vertx.ext.web.Router;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.core.backend.models.Collections;
import org.core.backend.models.Status;

import org.utils.backend.utils.SystemTasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.utils.backend.utils.Utils;

import io.vertx.core.http.HttpServerResponse;

import io.vertx.ext.web.RoutingContext;


/**
 * The Listings service v2 for managing marketplace listings.
 * Handles creation, reading, updating, and searching of listings with support
 * for custom fields.
 */
public class ListingsServiceV2 extends OrganisationService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        ListingsService.class.getName());

    /**
     * Sets routes for the HTTP server.
     * @param router The router used to set paths.
     */
    protected void setListingsV2Routes(final Router router) {
        this.logger.info("setListingsV2Routes -> ()");

        router.post("/createListingTypes")
            .handler(this::createListingTypes);
        router.post("/listListingTypes")
            .handler(this::listListingTypes);

        router.post("/listListings")
            .handler(this::listListings);
        router.post("/createListings")
            .handler(this::createListings);
        router.post("/updateListings")
            .handler(this::updateListings);
        router.post("/createAmenities")
            .handler(this::createAmenities);
        router.post("/listAmenities")
            .handler(this::listAmenities);
        router.post("/updateAmenities")
            .handler(this::updateAmenities);
        router.post("/addToFavourites")
            .handler(this::addToFavourites);
        router.post("/writeReviews")
            .handler(this::writeReviews);
        router.post("/listReviews")
            .handler(this::listReviews);
        router.post("/listFavourites")
            .handler(this::listFavourites);

        // Call parent organization service routes
        this.serOrganisationService(router);

    }

    /**
     * Searches for a valid booking spot.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createListingTypes")
    protected void createListingTypes(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListingTypes", rc,
            (xusr, body, params, headers, resp) -> {

                body.put("isActive", true);
                this.getUtils().putInsertDate(body);

                this.getUtils().assignRoleSaveFilters(xusr, body);

                this.getDbUtils().save(
                    Collections.LISTING_TYPES.toString(),
                        body, headers, resp);
        });
    }

    /**
     * List listing types.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "createListingTypes")
    private void listListingTypes(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListingTypes", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().assignRoleQueryFilters(
                    xusr, body, false);

                this.getDbUtils().find(
                    Collections.LISTING_TYPES.toString(), body, resp);
        });
    }

    /**
     * Lists the listings.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "listListings")
    private void listListings(final RoutingContext rc) {
        this.getUtils().execute3(MODULE + "listListings", rc,
            (xusr, body, params, headers, resp) -> {


                this.getDbUtils().find(
                    Collections.LISTINGS.toString(), body, resp);
        });
    }

    /**
     * Creates the listings.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "createListings")
    private void createListings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListings", rc,
            (xusr, body, params, headers, resp) -> {
                try {

                    this.validatePremiumArrays(body,
                        body.getJsonArray("amenities", new JsonArray()),
                    "amenities", resp);

                    JsonArray dicounts = body.getJsonArray(
                        "discounts", new JsonArray());
                    this.validateDiscounts(dicounts);
                    this.validatePremiumArrays(body, dicounts,
                        "discounts", resp);

                    this.validatePremiumArrays(body,
                        body.getJsonArray("loadings", new JsonArray()),
                    "loadings", resp);

                    this.validatePremiumArrays(body,
                        body.getJsonArray("statutoryPremiums", new JsonArray()),
                    "statutoryPremiums", resp);

                    body.put("status", Status.PENDING.name());
                    this.createPremiumObj(body,
                        body.getDouble("amount", Utils.ZERO_DOUBLE), resp);
                    body.remove("amount");

                    this.getUtils().assignRoleSaveFilters(xusr, body);
                    this.getDbUtils().save(Collections.LISTINGS.toString(),
                        body, headers, resp);
                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }
        }, "longitude", "latitude", "name", "description", "amount",
            "listingType");
    }

    /**
     * Validates the Discounts.
     * @param discounts The list of dicounts appliable.
     */
    private void validateDiscounts(final JsonArray discounts) throws Exception {
        if (discounts != null && !discounts.isEmpty()) {
            boolean shouldProceed = true;
            for (int i = 0; i < discounts.size(); i++) {
                if (discounts.getJsonObject(i) != null) {
                    JsonObject discount = discounts.getJsonObject(i);
                    if (!this.getUtils().isValid(discount, "amount",
                        "name", "type")) {
                            shouldProceed = false;
                    }
                }
            }
            if (!shouldProceed) {
                throw new Exception("Discounts should have amount, name "
                    + "and type fields");
            }
        }
    }

    /**
     * Creates the listings.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "updateListings")
    private void updateListings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateListings", rc,
            (xusr, body, params, headers, resp) -> {
                try {
                    JsonObject qry = new JsonObject()
                        .put("_id", body.getString("_id"));

                    this.getDbUtils().findOne(
                        Collections.LISTINGS.toString(), qry, res -> {

                            if (res == null || res.isEmpty()) {
                                resp.end(this.getUtils().getResponse(
                                    Utils.ERR_502,
                                        "Listing is missing !!!").encode());
                            } else {
                                this.updateLising(res, body, resp);
                            }

                    }, resp);

                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }

        }, "_id");
    }


    /**
     * Creates the listings.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "approveListing")
    private void approveListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "approveListing", rc,
            (xusr, body, params, headers, resp) -> {
                try {
                    JsonObject qry = new JsonObject()
                        .put("_id", body.getString("_id"));

                    JsonObject update = new JsonObject()
                        .put("status", body.getString("status"))
                        .put("remarks", body.getValue("remarks"));

                    this.getUtils().addUserToObject(
                        "approvalBy", xusr, update);

                    update.getJsonObject("approvalBy")
                        .put("remarks", body.getValue("remarks"));
                    this.getDbUtils().findOneAndUpdate(
                        Collections.ORGANISATION.toString(),
                            qry, update, res -> {
                            resp.end(this.getUtils().getResponse(res).encode());
                            //send email over here

                        }, fail -> {
                            this.logger.error(fail.getMessage(), fail);
                            resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, fail.getMessage()).encode());
                        });
                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }

        }, "_id");
    }

    /**
     * Update sthe listing.
     * @param listing The listing param.
     * @param body The body from the FE
     * @param resp The server response.
     */
    protected void updateLising(final JsonObject listing,
        final JsonObject body, final HttpServerResponse resp) {
        try {
            JsonObject updates = body.copy();

            if (updates.containsKey("amenities")) {
                this.validatePremiumArrays(listing,
                    updates.getJsonArray("amenities", new JsonArray()),
                "amenities", resp);
            }

            if (updates.containsKey("discounts")) {
                this.validatePremiumArrays(listing,
                    updates.getJsonArray("discounts", new JsonArray()),
                "discounts", resp);
            }

            if (updates.containsKey("loadings")) {
                this.validatePremiumArrays(listing,
                    updates.getJsonArray("loadings", new JsonArray()),
                "loadings", resp);
            }

            if (updates.containsKey("statutoryPremiums")) {
                this.validatePremiumArrays(listing,
                    updates.getJsonArray("statutoryPremiums",
                        new JsonArray()), "statutoryPremiums", resp);
            }

            if (updates.containsKey("amount")) {
                this.createPremiumObj(listing,
                    updates.getDouble("amount"), resp);
            }

            JsonObject qry = new JsonObject()
                .put("_id", updates.getString("_id"));
            listing.remove("_id");

            this.getDbUtils().findOneAndUpdate(
                Collections.LISTINGS.toString(), qry, listing, resp);
        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
            resp.end(this.getUtils().getResponse(
                Utils.ERR_502, e.getMessage()).encode());
        }
    }

    /**
     * Validates the lists of amenities to be added.
     * @param body The body from the FE
     * @param premiums The premiums list
     * @param field The field name to look for.
     * @param resp The server response
     */
    protected void validatePremiumArrays(final JsonObject body,
        final JsonArray premiums, final String field,
        final HttpServerResponse resp) {

            try {
                if (premiums != null && !premiums.isEmpty()) {

                    for (int i = Utils.ZERO; i < premiums.size(); i++) {
                        JsonObject premium = premiums.getJsonObject(i);
                        if (premium != null && !premium.isEmpty()) {
                            if (!this.getUtils().isValid(
                                premium, "name", "isAmount", "amount")) {
                                resp.end(this.getUtils().getResponse(
                                    Utils.ERR_502,
                                    "All " + field + " should have name, "
                                        + "isAmount and amount fields")
                                .encode());
                                break;
                            }
                        }
                    }
                    body.put("premium", body.getJsonObject(
                        "premium", new JsonObject())
                            .put(field, premiums));
                } else {

                    body.put("premium", body.getJsonObject(
                        "premium", new JsonObject())
                            .put(field, new JsonArray()));
                }

            } catch (final Exception e) {
                this.logger.error(e.getMessage(), e);
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            }
    }

    /**
     * Creates the premium object.
     * @param listing The listing from the db
     * @param amount The amount passed.
     * @param resp The server response.
     */
    protected void createPremiumObj(final JsonObject listing,
        final double amount, final HttpServerResponse resp) {
        listing.put("premium", listing.getJsonObject(
                "premium", new JsonObject())
            .put("basicPremium", amount));
    }

    /**
     * Creates the amenities to be added.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "createAmenities")
    private void createAmenities(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListingTypes", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().putInsertDate(body);
                body.put("status", Status.ACTIVE);
                this.getDbUtils().save(
                    Collections.AMENITIES.toString(),
                        body, headers, resp);
        });
    }


    /**
     * Creates the amenities to be added.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "listAmenities")
    private void listAmenities(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listAmenities", rc,
            (xusr, body, params, headers, resp) -> {

                this.getDbUtils().aggregate(
                    Collections.LISTINGS.toString(),
                        createAggregateQueryListListings(body), resp);
        });
    }


    /**
     * Creates the amenities to be added.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "updateAmenities")
    private void updateAmenities(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateAmenities", rc,
            (xusr, body, params, headers, resp) -> {

                JsonObject qry = new JsonObject()
                    .put("_id", body.getString("_id"));
                body.remove("_id");

                this.getDbUtils().findOneAndUpdate(
                    Collections.AMENITIES.toString(), qry, body, resp);
        }, "_id", "update");
    }

    /**
     * Writes reviews.
     * @param rc the routing context.
     */
    @SystemTasks(task = MODULE + "writeReviews")
    private void writeReviews(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "writeReviews", rc,
            (xusr, body, params, headers, resp) -> {

                this.getDbUtils().save(
                    Collections.REVIEWS.toString(), body, headers, resp);
        });
    }

    /**
     * Adds To Favourites.
     * @param rc the routing context.
     */
    @SystemTasks(task = MODULE + "addToFavoutites")
    private void addToFavourites(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "addToFavoutites", rc,
            (xusr, body, params, headers, resp) -> {

                this.getDbUtils().save(
                    Collections.FAVOURITES.toString(),
                        body, headers, resp);
        });
    }

    /**
     * Lists reviews.
     * @param rc the routing context.
     */
    @SystemTasks(task = MODULE + "listReviews")
    private void listReviews(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listReviews", rc,
            (xusr, body, params, headers, resp) -> {

                this.getDbUtils().find(
                    Collections.REVIEWS.toString(), body, resp);
        });
    }

    /**
     * Lists the favourites.
     * @param rc the routing context.
     */
    @SystemTasks(task = MODULE + "listFavourites")
    private void listFavourites(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listFavourites", rc,
            (xusr, body, params, headers, resp) -> {

                body.put("feduid", xusr.getString("feduid"));
                this.getDbUtils().find(
                    Collections.REVIEWS.toString(), body, resp);
        });
    }


    /**
     * Creates the aggregate query for listing.
     * @param body The body by the FE
     * @return pipeline for the query sent
     */
    private JsonArray createAggregateQueryListListings(final JsonObject body) {
        this.logger.info("createAggregateQueryListListings -> ()");
        // Add search functionality for custom fields
        this.getUtils().addFieldsToSearchQuery(body);

        JsonObject listingTypeLookUp = new JsonObject()
            .put("from", Collections.LISTING_TYPES.toString())
            .put("localField", "listingType")
            .put("foreignField", "_id")
            .put("as", "listingType");

        return new JsonArray()
            .add(new JsonObject()
                .put("$match", body))
            .add(new JsonObject()
                .put("$lookup", listingTypeLookUp))
            .add(new JsonObject()
                .put("$unwind", "$listingType"));
    }
}
