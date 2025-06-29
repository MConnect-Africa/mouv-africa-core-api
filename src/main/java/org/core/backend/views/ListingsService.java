package org.core.backend.views;

import io.vertx.ext.web.Router;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.UUID;
import org.core.backend.models.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

/**
 * The Listings service for managing marketplace listings.
 * Handles creation, reading, updating, and searching of listings with support
 * for custom fields.
 */
public class ListingsService extends ListingsServiceV2 {

    /**
     * Maximum percentage value for discounts.
     */
    private static final int MAX_PERCENTAGE_DISCOUNT = 100;

    /**
     * Default page limit for general listings.
     */
    private static final int DEFAULT_PAGE_LIMIT = 10;

    /**
     * Default page limit for listing types.
     */
    private static final int DEFAULT_LISTING_TYPES_LIMIT = 15;

    /**
     * Default page limit for organization listings.
     */
    private static final int DEFAULT_ORG_LISTINGS_LIMIT = 25;

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
            ListingsService.class.getName());

        /**
         * Sets routes for the HTTP server.
         *
         * @param router The router used to set paths.
         */
        protected void setListingsRoutes(final Router router) {
                this.logger.info("setListingsRoutes -> ()");

                // // Listing management routes
                // router.post("/createListing")
                //         .handler(this::createListing);
                // router.post("/listListings")
                //         .handler(this::listListings);
                // router.post("/getListing")
                //         .handler(this::getListing);
                // router.post("/updateListing")
                //         .handler(this::updateListing);
                // router.post("/deleteListing")
                //         .handler(this::deleteListing);
                // router.post("/searchListings")
                //         .handler(this::searchListings);

                // // Listing types management routes
                // router.post("/createListingType")
                //         .handler(this::createListingType);
                // router.post("/listListingTypes")
                //         .handler(this::listListingTypes);
                // router.post("/updateListingType")
                //         .handler(this::updateListingType);
                // router.post("/deleteListingType")
                //         .handler(this::deleteListingType);

                // // Advanced listing operations
                // router.post("/listListingsByType")
                //         .handler(this::listListingsByType);
                // router.post("/listListingsByUser")
                //         .handler(this::listListingsByUser);
                // router.post("/listListingsByOrganisation")
                //         .handler(this::listListingsByOrganisation);

                // // Discount management routes
                // router.post("/createDiscount")
                //         .handler(this::createDiscount);
                // router.post("/listDiscounts")
                //         .handler(this::listDiscounts);
                // router.post("/updateDiscount")
                //         .handler(this::updateDiscount);
                // router.post("/deleteDiscount")
                //         .handler(this::deleteDiscount);
                // router.post("/applyDiscountToListing")
                //         .handler(this::applyDiscountToListing);
                // router.post("/removeDiscountFromListing")
                //         .handler(this::removeDiscountFromListing);

                // // Promotion management routes
                // router.post("/createPromotion")
                //         .handler(this::createPromotion);
                // router.post("/listPromotions")
                //         .handler(this::listPromotions);
                // router.post("/updatePromotion")
                //         .handler(this::updatePromotion);
                // router.post("/deletePromotion")
                //         .handler(this::deletePromotion);
                // router.post("/applyPromotionToListing")
                //         .handler(this::applyPromotionToListing);
                // router.post("/removePromotionFromListing")
                //         .handler(this::removePromotionFromListing);

                // // Advanced pricing operations
                // router.post("/getListingEffectivePrice")
                //         .handler(this::getListingEffectivePrice);
                // router.post("/listListingsWithActivePromotions")
                //         .handler(this::listListingsWithActivePromotions);

                // Call parent organization service routes
                this.setListingsV2Routes(router);

        }

    /**
     * Builds dynamic query from filters-supports all fields and operators.
     * @param filters The filters object from request body
     * @return JsonObject representing MongoDB query
     */
    private JsonObject buildDynamicQuery(final JsonObject filters) {
        JsonObject query = new JsonObject();
        for (String field : filters.fieldNames()) {
            Object filterValue = filters.getValue(field);
            if (filterValue instanceof JsonObject) {
                JsonObject filterObj = (JsonObject) filterValue;
                String operator = filterObj.getString("operator");
                Object value = filterObj.getValue("value");
                switch (operator) {
                    case "gte":
                        query.put(
                                field,
                                new JsonObject().put("$gte", value));
                        break;
                    case "gt":
                        query.put(
                                field,
                                new JsonObject().put("$gt", value));
                        break;
                    case "lte":
                        query.put(
                                field,
                                new JsonObject().put("$lte", value));
                        break;
                    case "lt":
                        query.put(
                                field,
                                new JsonObject().put("$lt", value));
                        break;
                    case "in":
                        query.put(
                                field,
                                new JsonObject().put("$in", value));
                        break;
                    case "regex":
                        JsonObject regexQuery = new JsonObject()
                                .put("$regex", value);
                        if (filterObj.containsKey("options")) {
                            regexQuery.put("$options",
                                    filterObj.getString("options"));
                        }
                        query.put(field, regexQuery);
                        break;
                    case "exists":
                        query.put(
                                field, new JsonObject().put("$exists", value));
                        break;
                    case "between":
                        JsonArray range = (JsonArray) value;
                        query.put(
                            field,
                            new JsonObject().put("$gte",
                                range.getValue(0)
                            ).put("$lte", range.getValue(1)));
                        break;
                    default:
                        query.put(field, value);
                        break;
                }
            } else {
                query.put(field, filterValue);
            }
        }
        return query;
    }

    /**
     * Creates a new listing with mandatory and custom fields.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createListing")
    private void createListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListing", rc,
            (xusr, body, params, headers, resp) -> {
            try {
                // Generate unique listing ID
                String listingId = UUID.randomUUID().toString();

                // Add mandatory system fields
                body// .put("_id", listingId)
                    .put("organizationId", xusr.getString("organisationId"))
                    .put("userId", xusr.getString("_id"))
                    .put("createdAt", Instant.now().toString())
                    .put("updatedAt", Instant.now().toString())
                    .put("status", "active")
                    .put("views", 0)
                    .put("featured", false);

                // Validate listing type exists
                JsonObject typeQuery = new JsonObject()
                    .put("_id", body.getString("listingType"))
                    .put("organizationId",
                        xusr.getString("organisationId"));

                this.getDbUtils().findOne(
                    Collections.LISTING_TYPES.toString(),
                    typeQuery, typeResult -> {
                    if (typeResult == null || typeResult.isEmpty()) {
                        resp.end(this.getUtils().getResponse(
                            Utils.ERR_404,
                            "Listing type not found")
                        .encode());
                    } else {
                        // Save listing with all fields
                        // (including custom fields)
                        this.getUtils().assignRoleSaveFilters(
                            xusr, body);
                        this.getDbUtils().save(
                            Collections.LISTINGS.toString(),
                            body, headers, resp);
                        }
                    }, resp);

                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502, e.getMessage()).encode());
                }
        }, "listingType", "title", "amount");
    }

    /**
     * Lists listings with filtering support.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListings")
    private void listListings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListings", rc,
            (xusr, body, params, headers, resp) -> {
            // Extract pagination parameters
            int limit = body.getInteger("limit", DEFAULT_PAGE_LIMIT);
            int offset = body.getInteger("offset", 0);

            // Extract filters object and build dynamic query
            JsonObject filters = body.getJsonObject(
                "filters", new JsonObject());
            JsonObject query = this.buildDynamicQuery(filters);

            // Default to active status if not specified
            if (!query.containsKey("status")) {
                query.put("status", "active");
            }

            // Apply role-based query filters
            this.getUtils().assignRoleQueryFilters(xusr, query, false);

            // Add default sorting if not specified
            if (!query.containsKey("sort")) {
                query.put("sort", new JsonObject()
                    .put("featured", -1)
                    .put("createdAt", -1));
                }

             // Add pagination
            query.put("limit", limit);
            query.put("offset", offset);

            this.getDbUtils().find(
                Collections.LISTINGS.toString(), query, resp);
        });
    }

    /**
     * Gets a specific listing by ID.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "getListing")
    private void getListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "getListing", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    this.getDbUtils().findOne(Collections.LISTINGS.toString(),
                            query, result -> {
                                if (result == null || result.isEmpty()) {
                                    resp.end(this.getUtils().getResponse(
                                            Utils.ERR_404,
                                            "Listing not found").encode());
                                } else {
                                    // Increment view count if not the owner
                                    if (!result.getString("userId", "")
                                            .equals(xusr.getString(
                                                    "_id"))) {
                                        this.incrementListingViews(
                                                result.getString(
                                                        "_id"));
                                    }
                                    resp.end(this.getUtils()
                                            .getResponse(result).encode());
                                }
                            }, resp);
                }, "_id");
    }

    /**
     * Updates a listing while preserving custom fields.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "updateListing")
    private void updateListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateListing", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        // Prepare update data by removing system fields
                        // that shouldn't be updated
                        JsonObject updateData = body.copy();
                        updateData.remove("organizationId");
                        updateData.remove("userId");
                        updateData.remove("createdAt");
                        updateData.remove("views");
                        updateData.remove("_id");

                        // Add system update timestamp
                        updateData.put("updatedAt", Instant.now().toString());

                        // Build query with ownership validation
                        JsonObject query = new JsonObject()
                                .put("_id", body.getString("_id"));

                        // Apply role-based filters
                        // (users can only update their own listings)
                        this.getUtils().assignRoleQueryFilters(
                                xusr, query, false);

                        // Additional owner check for non-admin users
                        if (!this.getUtils().isRole("admin", xusr)
                                && !this.getUtils().isRole("superadmin",
                                        xusr)) {
                            query.put("userId", xusr.getString("_id"));
                        }

                        this.getDbUtils().findOneAndUpdate(
                                Collections.LISTINGS.toString(),
                                query, new JsonObject().put("$set", updateData),
                                result -> {
                                    if (result == null || result.isEmpty()) {
                                        resp.end(this.getUtils().getResponse(
                                                Utils.ERR_404,
                                                "Listing not found or access "
                                                        + "denied")
                                                .encode());
                                    } else {
                                        resp.end(this.getUtils()
                                                .getResponse(result)
                                                .encode());
                                    }
                                }, fail -> {
                                    this.logger.error(fail.getMessage(), fail);
                                    resp.end(this.getUtils().getResponse(
                                            Utils.ERR_502,
                                            fail.getMessage()).encode());
                                });

                    } catch (final Exception e) {
                        this.logger.error(e.getMessage(), e);
                        resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                }, "_id");
    }

    /**
     * Deletes a listing (soft delete by setting status to inactive).
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "deleteListing")
    private void deleteListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "deleteListing", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    // Apply role-based filters and ownership check
                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    if (!this.getUtils().isRole("admin", xusr)
                            && !this.getUtils().isRole("superadmin", xusr)) {
                        query.put("userId", xusr.getString("_id"));
                    }

                    JsonObject update = new JsonObject()
                            .put("$set", new JsonObject()
                                    .put("status", "inactive")
                                    .put("deletedAt", Instant.now().toString())
                                    .put("deletedBy", xusr.getString("_id")));

                    this.getDbUtils().findOneAndUpdate(
                            Collections.LISTINGS.toString(),
                            query, update, result -> {
                                if (result == null || result.isEmpty()) {
                                    resp.end(this.getUtils().getResponse(
                                            Utils.ERR_404,
                                            "Listing not found or access "
                                                    + "denied")
                                            .encode());
                                } else {
                                    resp.end(this.getUtils().getResponse(
                                            new JsonObject().put("message",
                                                    "Listing deleted "
                                                            + "successfully"))
                                            .encode());
                                }
                            }, fail -> {
                                this.logger.error(fail.getMessage(), fail);
                                resp.end(this.getUtils().getResponse(
                                        Utils.ERR_502,
                                        fail.getMessage()).encode());
                            });
                }, "_id");
    }

    /**
     * Searches listings with support for custom fields.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "searchListings")
    private void searchListings(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchListings", rc,
                (xusr, body, params, headers, resp) -> {
                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(
                            xusr, body, false);

                    // Add search functionality for custom fields
                    this.getUtils().addFieldsToSearchQuery(body);

                    // Default to active listings only
                    if (!body.containsKey("status")) {
                        body.put("status", "active");
                    }

                    // Add default sorting by relevance and date
                    if (!body.containsKey("sort")) {
                        body.put("sort", new JsonObject()
                                .put("featured", -1)
                                .put("createdAt", -1));
                    }

                    this.getDbUtils().find(Collections.LISTINGS.toString(),
                            body, resp);
                }, "searchTerm", "fieldsToSearchFor");
    }

    /**
     * Creates a new listing type.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createListingType")
    private void createListingType(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createListingType", rc,
                (xusr, body, params, headers, resp) -> {
                    // Add system fields
                    body.put("organizationId", xusr.getString("organisationId"))
                        .put("createdBy", xusr.getString("_id"))
                        .put("createdAt", Instant.now().toString())
                        .put("updatedAt", Instant.now().toString())
                        .put("isActive", true);

                    this.getUtils().assignRoleSaveFilters(xusr, body);
                    this.getDbUtils().save(Collections.LISTING_TYPES.toString(),
                            body, headers, resp);
                }, "name", "description");
    }

    /**
     * Lists listing types.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListingTypes")
    private void listListingTypes(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListingTypes", rc,
            (xusr, body, params, headers, resp) -> {
                    // Extract pagination parameters
            int limit = body.getInteger("limit", DEFAULT_PAGE_LIMIT);
            int offset = body.getInteger("offset", 0);

            // Extract filters object and build dynamic query
            JsonObject filters = body.getJsonObject(
                "filters", new JsonObject());
            JsonObject query = this.buildDynamicQuery(filters);

            // Default to active status if not specified
            if (!query.containsKey("status")
            && !query.containsKey("isActive")) {
                query.put("isActive", true);
            }

            // Map status to isActive for database compatibility
            if (query.containsKey("status")) {
                String status = query.getString("status");
                query.remove("status");
                query.put("isActive", "active".equals(status));
            }

            // Apply role-based query filters
            this.getUtils().assignRoleQueryFilters(xusr, query, false);

            // Add pagination
            query.put("limit", limit);
            query.put("offset", offset);

            this.getDbUtils().find(Collections.LISTING_TYPES.toString(),
                query, resp);
        });
    }

    /**
     * Updates a listing type.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "updateListingType")
    private void updateListingType(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateListingType", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject updateData = body.getJsonObject("update",
                            new JsonObject());
                    updateData.put("updatedAt", Instant.now().toString());

                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    this.getUtils().assignRoleQueryFilters(xusr, query, true);

                    this.getDbUtils().findOneAndUpdate(
                            Collections.LISTING_TYPES.toString(),
                            query, new JsonObject().put("$set", updateData),
                            resp);
                }, "_id", "update");
    }

    /**
     * Deletes a listing type (soft delete).
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "deleteListingType")
    private void deleteListingType(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "deleteListingType", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    this.getUtils().assignRoleQueryFilters(xusr, query, true);

                    JsonObject update = new JsonObject()
                            .put("$set", new JsonObject()
                                    .put("isActive", false)
                                    .put("deletedAt",
                                            Instant.now().toString()));

                    this.getDbUtils().findOneAndUpdate(
                            Collections.LISTING_TYPES.toString(),
                            query, update, resp);
                }, "_id");
    }

    /**
     * Lists listings by specific type ID.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListingsByType")
    private void listListingsByType(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListingsByType", rc,
                (xusr, body, params, headers, resp) -> {
                    // Extract pagination parameters
                    int limit = body.getInteger(
                        "limit", DEFAULT_LISTING_TYPES_LIMIT);
                    int offset = body.getInteger("offset", 0);

                    // Extract filters and build dynamic query
                    JsonObject filters = body.getJsonObject(
                        "filters", new JsonObject());
                    JsonObject query = this.buildDynamicQuery(filters);

                    // Set required listingType parameter
                    query.put("listingType", body.getString("listingType"));

                    // Default to active status if not specified
                    if (!query.containsKey("status")) {
                        query.put("status", "active");
                    }

                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    // Add default sorting
                    if (!query.containsKey("sort")) {
                        query.put("sort", new JsonObject()
                                .put("featured", -1)
                                .put("createdAt", -1));
                    }

                    // Add pagination
                    query.put("limit", limit);
                    query.put("offset", offset);

                    this.getDbUtils().find(Collections.LISTINGS.toString(),
                            query, resp);
                }, "listingType");
    }

    /**
     * Lists listings by specific user ID.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListingsByUser")
    private void listListingsByUser(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListingsByUser", rc,
            (xusr, body, params, headers, resp) -> {
            // Extract pagination parameters
            int limit = body.getInteger("limit", DEFAULT_PAGE_LIMIT);
            int offset = body.getInteger("offset", 0);

            String targetUserId = body.getString(
                "userId", xusr.getString("_id"));
            // Users can only see their own listings unless they're admin
            if (!this.getUtils().isRole("admin", xusr)
                && !this.getUtils().isRole("superadmin", xusr)) {
                targetUserId = xusr.getString("_id");
            }

            // Extract filters and build dynamic query
            JsonObject filters = body.getJsonObject(
                "filters", new JsonObject());
            JsonObject query = this.buildDynamicQuery(filters);

            // Set required userId parameter
            query.put("userId", targetUserId);

            // Apply role-based query filters
            this.getUtils().assignRoleQueryFilters(xusr, query, false);

            // Add default sorting
            if (!query.containsKey("sort")) {
                query.put("sort", new JsonObject().put("createdAt", -1));
            }

            // Add pagination
            query.put("limit", limit);
            query.put("offset", offset);

            this.getDbUtils().find(Collections.LISTINGS.toString(),
                query, resp);
        });
    }

    /**
     * Lists listings by organization.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListingsByOrganisation")
    private void listListingsByOrganisation(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListingsByOrganisation", rc,
            (xusr, body, params, headers, resp) -> {
            // Extract pagination parameters
            int limit = body.getInteger(
                "limit", DEFAULT_ORG_LISTINGS_LIMIT);
            int offset = body.getInteger("offset", 0);

            String targetOrganisationId = body.getString(
                "organisationId", xusr.getString("organisationId"));

            // Users can only see their own organisation's
            // listings unless they're admin
            if (!this.getUtils().isRole("admin", xusr)
                && !this.getUtils().isRole("superadmin", xusr)) {
                targetOrganisationId = xusr.getString("organisationId");
            }

            // Extract filters and build dynamic query
            JsonObject filters = body.getJsonObject(
                "filters", new JsonObject());
            JsonObject query = this.buildDynamicQuery(filters);

            // Set required organizationId parameter
            query.put("organizationId", targetOrganisationId);

            // Apply role-based query filters
            this.getUtils().assignRoleQueryFilters(xusr, query, false);

            // Add default sorting
            if (!query.containsKey("sort")) {
                query.put("sort", new JsonObject()
                    .put("featured", -1)
                    .put("createdAt", -1));
            }

            // Add pagination
            query.put("limit", limit);
            query.put("offset", offset);

            this.getDbUtils().find(Collections.LISTINGS.toString(),
                query, resp);
        });
    }

    /**
     * Helper method to increment listing view count.
     *
     * @param listingId The listing ID to increment views for.
     */
    private void incrementListingViews(final String listingId) {
        JsonObject query = new JsonObject().put("_id", listingId);
        JsonObject update = new JsonObject()
                .put("$inc", new JsonObject().put("views", 1));

        this.getDbUtils().update(Collections.LISTINGS.toString(), query, update,
                () -> {
                    // View incremented successfully
                    this.logger.debug("Incremented views for listing: "
                            + listingId);
                },
                error -> {
                    // Log error but don't fail the main request
                    this.logger.error("Failed to increment views for listing: "
                            + listingId, error);
                });
    }

    // ======================================================================
    // DISCOUNT MANAGEMENT METHODS
    // ======================================================================

    /**
     * Creates a new discount.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createDiscount")
    private void createDiscount(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createDiscount", rc,
        (xusr, body, params, headers, resp) -> {
        try {
            // Generate unique discount ID
            String discountId = UUID.randomUUID().toString();

            // Add mandatory system fields
            body.put("_id", discountId)
                .put("organizationId", xusr.getString("organisationId"))
                .put("createdBy", xusr.getString("_id"))
                .put("createdAt", Instant.now().toString())
                .put("updatedAt", Instant.now().toString())
                .put("status", "active");

            // Validate discount type and value
            String discountType = body.getString(
                "type", "percentage");
            Double discountValue = body.getDouble("value");

            if (discountValue == null
                || discountValue <= 0) {
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502,
                        "Discount value must be greater than 0")
                        .encode());
            }

           if ("percentage".equals(discountType)
                && discountValue > MAX_PERCENTAGE_DISCOUNT) {
                resp.end(this.getUtils().getResponse(
                    Utils.ERR_502,
                    "Percentage discount cannot exceed 100%")
                    .encode());
            }

            // Save discount
            this.getUtils().assignRoleSaveFilters(xusr, body);
            this.getDbUtils().save(Collections.DISCOUNTS.toString(),
                body, headers, resp);

        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
            resp.end(this.getUtils().getResponse(
                Utils.ERR_502, e.getMessage()).encode());
        }
        }, "name", "type", "value");
    }

    /**
     * Lists discounts with filtering support.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listDiscounts")
    private void listDiscounts(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listDiscounts", rc,
                (xusr, body, params, headers, resp) -> {
                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, body, false);

                    this.getDbUtils().find(Collections.DISCOUNTS.toString(),
                            body, resp);
                });
    }

    /**
     * Updates a discount.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "updateDiscount")
    private void updateDiscount(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateDiscount", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        // Remove _id from update body and set updatedAt
                        String discountId = body.getString("_id");
                        body.remove("_id");
                        body.put("updatedAt", Instant.now().toString());

                        JsonObject query = new JsonObject()
                                .put("_id", discountId);

                        // Apply role-based query filters
                        this.getUtils().assignRoleQueryFilters(xusr, query,
                                false);

                        this.getDbUtils().update(
                                Collections.DISCOUNTS.toString(), query, body,
                                resp);

                    } catch (final Exception e) {
                        this.logger.error(e.getMessage(), e);
                        resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                }, "_id");
    }

    /**
     * Deletes a discount (soft delete).
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "deleteDiscount")
    private void deleteDiscount(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "deleteDiscount", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    JsonObject update = new JsonObject()
                            .put("status", "deleted")
                            .put("deletedAt", Instant.now().toString())
                            .put("deletedBy", xusr.getString("_id"));

                    this.getDbUtils().update(
                            Collections.DISCOUNTS.toString(), query, update,
                            resp);
                }, "_id");
    }

    /**
     * Applies a discount to a listing.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "applyDiscountToListing")
    private void applyDiscountToListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "applyDiscountToListing", rc,
        (xusr, body, params, headers, resp) -> {
        try {
            String listingId = body.getString("listingId");
            String discountId = body.getString("discountId");
            String orgId = xusr.getString("organisationId");
            String now = Instant.now().toString();

            // Check if listing exists and user has access
            JsonObject listingQuery = new JsonObject()
                .put("_id", listingId);
            this.getUtils().assignRoleQueryFilters(xusr,
                listingQuery, false);

            this.getDbUtils().findOne(
                Collections.LISTINGS.toString(),
                listingQuery,
                listing -> {
                if (listing == null || listing.isEmpty()) {
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_404,
                        "Listing not found").encode());
                    return;
                }

             // Check if discount exists and is active
            JsonObject discountQuery = new JsonObject()
                .put("_id", discountId)
                .put("status", "active");
            this.getUtils().assignRoleQueryFilters(xusr,
                discountQuery, false);
            this.getDbUtils().findOne(
                Collections.DISCOUNTS.toString(),
                discountQuery, discount -> {
                if (discount == null || discount.isEmpty()) {
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_404,
                        "Active discount not found").encode());
                    return;
                }

                // Create listing-discount relationship
                JsonObject lsDiscount = new JsonObject()
                    .put("_id", UUID.randomUUID().toString())
                    .put("listingId", listingId)
                    .put("discountId", discountId)
                    .put("organizationId", orgId)
                    .put("appliedBy", xusr.getString("_id"))
                    .put("appliedAt", now)
                    .put("status", "active");

                this.getUtils().assignRoleSaveFilters(xusr, lsDiscount);
                this.getDbUtils().save(
                    Collections.LISTING_DISCOUNTS.toString(),
                    lsDiscount,
                    headers,
                    resp);
                }, resp);
             }, resp);

        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
            resp.end(this.getUtils().getResponse(
                Utils.ERR_502, e.getMessage()).encode());
            }
        }, "listingId", "discountId");
    }

    /**
     * Removes a discount from a listing.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "removeDiscountFromListing")
    private void removeDiscountFromListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "removeDiscountFromListing", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("listingId", body.getString("listingId"))
                            .put("discountId", body.getString("discountId"))
                            .put("status", "active");

                    this.getUtils().assignRoleQueryFilters(xusr, query,
                            false);

                    JsonObject update = new JsonObject()
                            .put("status", "removed")
                            .put("removedAt", Instant.now().toString())
                            .put("removedBy", xusr.getString("_id"));

                    this.getDbUtils().update(
                            Collections.LISTING_DISCOUNTS.toString(),

                            query, update, resp);
                }, "listingId", "discountId");
    }

    // ==============================================================
    // PROMOTION MANAGEMENT METHODS
    // ==============================================================

    /**
     * Creates a new promotion.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createPromotion")
    private void createPromotion(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createPromotion", rc,
            (xusr, body, params, headers, resp) -> {
            try {
                // Generate unique promotion ID
                String promotionId = UUID.randomUUID().toString();

                // Add mandatory system fields
                body.put("_id", promotionId)
                .put("organizationId",
                    xusr.getString("organisationId"))
                .put("createdBy", xusr.getString("_id"))
                .put("createdAt", Instant.now().toString())
                .put("updatedAt", Instant.now().toString())
                .put("status", "active");

                // Validate start and end dates
                String startDate = body.getString("startDate");
                String endDate = body.getString("endDate");

                if (startDate != null
                    && endDate != null) {
                    try {
                        Instant start = Instant.parse(startDate);
                        Instant end = Instant.parse(endDate);
                        if (end.isBefore(start)) {
                            resp.end(this.getUtils().getResponse(
                                Utils.ERR_502,
                                "End date must be after start date")
                                .encode());
                                return;
                        }
                    } catch (Exception dateEx) {
                        resp.end(this.getUtils().getResponse(
                            Utils.ERR_502,
                            "Invalid date format. Use ISO 8601 format")
                        .encode());
                        return;
                    }
                }

         // Save promotion
            this.getUtils().assignRoleSaveFilters(xusr, body);
            this.getDbUtils().save(
                Collections.PROMOTIONS.toString(), body, headers, resp);

        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
            resp.end(this.getUtils().getResponse(
                Utils.ERR_502, e.getMessage()).encode());
        }
        }, "name", "type");
    }

    /**
     * Lists promotions with filtering support.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listPromotions")
    private void listPromotions(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listPromotions", rc,
                (xusr, body, params, headers, resp) -> {
                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, body, false);

                    this.getDbUtils().find(Collections.PROMOTIONS.toString(),
                            body, resp);
                });
    }

    /**
     * Updates a promotion.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "updatePromotion")
    private void updatePromotion(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updatePromotion", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        // Remove _id from update body and set updatedAt
                        String promotionId = body.getString("_id");
                        body.remove("_id");
                        body.put("updatedAt", Instant.now().toString());

                        JsonObject query = new JsonObject()
                                .put("_id", promotionId);

                        // Apply role-based query filters
                        this.getUtils().assignRoleQueryFilters(
                                xusr, query, false);

                        String items = Collections.PROMOTIONS.toString();
                        this.getDbUtils().update(items, query, body, resp);

                    } catch (final Exception e) {
                        this.logger.error(e.getMessage(), e);
                        resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                }, "_id");
    }

    /**
     * Deletes a promotion (soft delete).
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "deletePromotion")
    private void deletePromotion(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "deletePromotion", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("_id", body.getString("_id"));

                    // Apply role-based query filters
                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    JsonObject update = new JsonObject()
                            .put("status", "deleted")
                            .put("deletedAt", Instant.now().toString())
                            .put("deletedBy", xusr.getString("_id"));
                    String items = Collections.PROMOTIONS.toString();
                    this.getDbUtils().update(items, query, update, resp);
                }, "_id");
    }

    /**
     * Applies a promotion to a listing.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "applyPromotionToListing")
    private void applyPromotionToListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "applyPromotionToListing", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        String listingId = body.getString("listingId");
                        String promotionId = body.getString("promotionId");

                        // Check if listing exists and user has access
                        JsonObject listingQuery = new JsonObject()
                                .put("_id", listingId);
                        this.getUtils().assignRoleQueryFilters(
                                xusr, listingQuery, false);

                        String listings = Collections.LISTINGS.toString();
                        String promotions = Collections.PROMOTIONS.toString();
                        String cols = Collections.LISTING_PROMOTIONS.toString();
                        String orgId = xusr.getString("organisationId");
                        String now = Instant.now().toString();

                        this.getDbUtils().findOne(
                                listings, listingQuery, listing -> {
                                    if (listing == null || listing.isEmpty()) {
                                        resp.end(this.getUtils().getResponse(
                                                Utils.ERR_404,
                                                "Listing not found")
                                                .encode());
                                        return;
                                    }

                                    // Check if promotion exists and is active
                                    JsonObject promotionQuery = new JsonObject()
                                            .put("_id", promotionId)
                                            .put("status", "active");
                                    this.getUtils().assignRoleQueryFilters(xusr,
                                            promotionQuery, false);

                                this.getDbUtils().findOne(
                                    promotions,
                                    promotionQuery, item -> {
                                    if (item == null || item.isEmpty()) {
                                        resp.end(this.getUtils()
                                            .getResponse(
                                                Utils.ERR_404,
                                                "Active promotion not found")
                                            .encode());
                                        return;
                                    }

                                // Create listing-promotion relationship
                                JsonObject lsPromotion = new JsonObject()
                                    .put("_id", UUID.randomUUID()
                                    .toString())
                                    .put("listingId", listingId)
                                    .put("promotionId", promotionId)
                                    .put("organizationId", orgId)
                                    .put("appliedBy",
                                        xusr.getString("_id"))
                                    .put("appliedAt", now)
                                    .put("status", "active");

                                this.getUtils().assignRoleSaveFilters(
                                    xusr,
                                    lsPromotion);
                                this.getDbUtils().save(
                                    cols, lsPromotion, headers, resp);
                            }, resp);
                        }, resp);

                    } catch (final Exception e) {
                        this.logger.error(e.getMessage(), e);
                        resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                }, "listingId", "promotionId");
    }

    /**
     * Removes a promotion from a listing.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "removePromotionFromListing")
    private void removePromotionFromListing(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "removePromotionFromListing", rc,
                (xusr, body, params, headers, resp) -> {
                    JsonObject query = new JsonObject()
                            .put("listingId", body.getString("listingId"))
                            .put("promotionId", body.getString("promotionId"))
                            .put("status", "active");

                    this.getUtils().assignRoleQueryFilters(xusr, query, false);

                    JsonObject update = new JsonObject()
                            .put("status", "removed")
                            .put("removedAt", Instant.now().toString())
                            .put("removedBy", xusr.getString("_id"));

                    this.getDbUtils().update(
                            Collections.LISTING_PROMOTIONS.toString(),
                            query, update, resp);
                }, "listingId", "promotionId");
    }

    // ==========================================================
    // ADVANCED PRICING METHODS
    // ==========================================================

    /**
     * Calculates the effective price of a listing after applying discounts and
     * promotions.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "getListingEffectivePrice")
    private void getListingEffectivePrice(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "getListingEffectivePrice", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        String listingId = body.getString("listingId");

                        // Get listing
                        JsonObject listingQuery = new JsonObject()
                                .put("_id", listingId);
                        this.getUtils().assignRoleQueryFilters(
                                xusr, listingQuery, false);

                        this.getDbUtils().findOne(
                                Collections.LISTINGS.toString(),
                                listingQuery, item -> {
                                    if (item == null || item.isEmpty()) {
                                        resp.end(this.getUtils().getResponse(
                                                Utils.ERR_404,
                                                "Listing not found")
                                                .encode());
                                        return;
                                    }

                        Double baseAmount = item.getDouble("amount");
                        if (baseAmount == null) {
                            resp.end(this.getUtils().getResponse(
                                Utils.ERR_502,
                                "Listing has no amount set")
                                .encode());
                            return;
                        }

                        // Get active discounts for this listing
                        JsonObject discountQuery = new JsonObject()
                            .put("listingId", listingId)
                            .put("status", "active");

                        this.getDbUtils().find(
                            Collections.LISTING_DISCOUNTS
                                .toString(),
                            discountQuery,
                            discountResults -> {
                                try {
                                    JsonArray items = new JsonArray(
                                        discountResults);
                                    Double effectivePrice = baseAmount;

                                // Apply discounts
                                    for (int i = 0; i < items
                                            .size(); i++) {
                                        JsonObject rel = items.getJsonObject(i);
                                        String itemId = rel.getString(
                                            "discountId");
                                        JsonObject query = new JsonObject()
                                            .put("_id", itemId);
                                    }

                                // Get active promotions for this listing
                                    JsonObject promotionQuery = new JsonObject()
                                        .put("listingId", listingId)
                                        .put("status", "active");

                                this.getDbUtils().find(
                                    Collections.LISTING_PROMOTIONS.toString(),
                                    promotionQuery,
                                    promotionResults -> {
                                        try {
                                            JsonObject result = new JsonObject()
                                                .put("listingId", listingId)
                                                .put("baseAmount", baseAmount)
                                                .put("effectivePrice",
                                                    effectivePrice)
                                                .put("discountsApplied",
                                                    items.size())
                                                .put("promotionsApplied",
                                                    promotionResults.size())
                                            .put("savings",
                                                baseAmount - effectivePrice);

                                            resp.end(this.getUtils()
                                                .getResponse(result).encode());

                                            resp.end(this.getUtils()
                                                .getResponse(result).encode());
                } catch (Exception e) {
                    this.logger.error(
                        e.getMessage(), e);
                    resp.end(this.getUtils()
                            .getResponse(
                                    Utils.ERR_502,
                                    e.getMessage())
                            .encode());
                }
            },
            resp);

                } catch (Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils().getResponse(
                        Utils.ERR_502,
                        e.getMessage()).encode());
                    return;
                }
            }, resp);
        }, resp);

                    } catch (final Exception e) {
                        this.logger.error(e.getMessage(), e);
                        resp.end(this.getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                }, "listingId");
    }

    /**
     * Lists listings that have active promotions.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listListingsWithActivePromotions")
    private void listListingsWithActivePromotions(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listListingsWithActivePromotions",
            rc,
        (xusr, body, params, headers, resp) -> {
            // Apply role-based query filters
            this.getUtils().assignRoleQueryFilters(xusr, body, false);

            // Query for listings that have active promotions
            JsonObject promotionQuery = new JsonObject()
                .put("status", "active");
            this.getUtils().assignRoleQueryFilters(
                xusr, promotionQuery, false);

            this.getDbUtils().find(
                Collections.LISTING_PROMOTIONS.toString(),
                promotionQuery,
                promotionResults -> {
                try {
                    JsonArray promotions = new JsonArray(
                        promotionResults);
                    JsonArray listingIds = new JsonArray();

                    for (int i = 0; i < promotions.size(); i++) {
                        JsonObject promotion = promotions
                            .getJsonObject(i);
                        String listingId = promotion
                            .getString("listingId");
                        if (!listingIds.contains(listingId)) {
                            listingIds.add(listingId);
                        }
                    }

                                    if (listingIds.isEmpty()) {
                                        resp.end(
                                                this.getUtils().getResponse(
                                                        new JsonArray())
                                                        .encode());
                                        return;
                                    }

                                    // Get listings
                                    JsonObject listingQuery = new JsonObject()
                                            .put("_id",
                                                    new JsonObject().put(
                                                            "$in",
                                                            listingIds));
                                    this.getUtils().assignRoleQueryFilters(
                                            xusr, listingQuery, false);

                                    this.getDbUtils().find(
                                            Collections.LISTINGS.toString(),
                                            listingQuery, resp);

                                } catch (Exception e) {
                                    this.logger.error(e.getMessage(), e);
                                    resp.end(this.getUtils().getResponse(
                                            Utils.ERR_502,
                                            e.getMessage()).encode());
                                }
                            }, resp);
                });
    }
}
