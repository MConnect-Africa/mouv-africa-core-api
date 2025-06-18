package org.core.backend.views;

import io.vertx.ext.web.Router;

import org.core.backend.models.Collections;
import org.core.backend.models.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.utils.backend.utils.Utils;
import org.utils.backend.utils.SystemTasks;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.RoutingContext;


/**
 * The Listings service v2 for managing marketplace listings.
 * Handles creation, reading, updating, and searching of listings with support
 * for custom fields.
 */
public class ListingsServiceV2 extends OrganisationService {

    /**
     * Maximum percentage value for discounts.
     */
    private static final int MAX_PERCENTAGE_DISCOUNT = 100;

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
        this.logger.info("setListingsRoutes -> ()");

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
        this.getUtils().execute2(MODULE + "listListings", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().assignRoleQueryFilters(
                    xusr, body, false);

                this.getDbUtils().find(
                    Collections.LISTING_TYPES.toString(), body, resp);
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

                    this.validatePremiumArrays(body,
                        body.getJsonArray("discounts", new JsonArray()),
                    "discounts", resp);

                    this.validatePremiumArrays(body,
                        body.getJsonArray("loadings", new JsonArray()),
                    "loadings", resp);

                    this.validatePremiumArrays(body,
                        body.getJsonArray("statutoryPremiums", new JsonArray()),
                    "statutoryPremiums", resp);

                    body.put("status", Status.ACTIVE.name());
                    this.createPremiumObj(body, resp);

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
     * Creates the listings.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "listListings")
    private void updateListings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListings", rc,
            (xusr, body, params, headers, resp) -> {
                try {

                    JsonObject updates = body.copy();

                    if (updates.containsKey("amenities")) {
                        this.validatePremiumArrays(body,
                            updates.getJsonArray("amenities", new JsonArray()),
                        "amenities", resp);
                    }

                    if (updates.containsKey("discounts")) {
                        this.validatePremiumArrays(body,
                            body.getJsonArray("discounts", new JsonArray()),
                        "discounts", resp);
                    }

                    if (updates.containsKey("loadings")) {
                        this.validatePremiumArrays(body,
                            body.getJsonArray("loadings", new JsonArray()),
                        "loadings", resp);
                    }

                    if (updates.containsKey("statutoryPremiums")) {
                        this.validatePremiumArrays(body,
                            body.getJsonArray("statutoryPremiums",
                                new JsonArray()), "statutoryPremiums", resp);
                    }

                    if (updates.containsKey("amount")) {
                        this.createPremiumObj(body, resp);
                    }

                    JsonObjct qry = new JsonObject()
                        .put("_id", body.getString("_id"));

                    updates.remove("_id");
                    this.getDbUtils().findOneAndUpdate(
                        Collections.LISTINGS.toString(), qry, updates, resp);
                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }

        }, "_id", "update");
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
                                        + "isAmount and amount foields")
                                .encode());
                                break;
                            }
                        }
                    }

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
     * @param body The body from the FE
     * @param resp The server response.
     */
    protected void createPremiumObj(final JsonObject body,
        final HttpServerResponse resp) {
        if (this.getUtils().isValid(body, "amount")) {
            body.put("premium", body.getJsonObject("premium", new JsonObject())
                .put("basicPremium", body.getDouble("amount", ZERO_DOUBLE)));
        } else {
            resp.end(this.getUtils().getResponse(Utils.ERR_503,
                "Field amount is missing when creating premiums").encode());
        }
    }
}
