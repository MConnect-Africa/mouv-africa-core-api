package org.core.backend.views;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.SystemTasks;
import org.core.backend.models.Collections;
import org.utils.backend.utils.Utils;

/**
 * The Listings service for managing marketplace listings.
 * Handles creation, reading, updating, and searching of listings with support
 * for custom fields.
 */
public class ListingsService extends OrganisationService {

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

		// Listing management routes
		router.post("/createListing")
				.handler(this::createListing);
		router.post("/listListings")
				.handler(this::listListings);
		router.post("/getListing")
				.handler(this::getListing);
		router.post("/updateListing")
				.handler(this::updateListing);
		router.post("/deleteListing")
				.handler(this::deleteListing);
		router.post("/searchListings")
				.handler(this::searchListings);

		// Listing types management routes
		router.post("/createListingType")
				.handler(this::createListingType);
		router.post("/listListingTypes")
				.handler(this::listListingTypes);
		router.post("/updateListingType")
				.handler(this::updateListingType);
		router.post("/deleteListingType")
				.handler(this::deleteListingType);

		// Advanced listing operations
		router.post("/listListingsByType")
				.handler(this::listListingsByType);
		router.post("/listListingsByUser")
				.handler(this::listListingsByUser);
		router.post("/listListingsByOrganisation")
				.handler(this::listListingsByOrganisation);

		// Call parent organization service routes
		this.setAdminRoutes(router);
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
						body.put("_id", listingId)
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
								.put("organizationId", xusr.getString("organisationId"));

						this.getDbUtils().findOne(Collections.LISTING_TYPES.toString(),
								typeQuery, typeResult -> {
									if (typeResult == null || typeResult.isEmpty()) {
										resp.end(this.getUtils().getResponse(
												Utils.ERR_404, "Listing type not found").encode());
									} else {
										// Save listing with all fields (including custom fields)
										this.getUtils().assignRoleSaveFilters(xusr, body);
										this.getDbUtils().save(Collections.LISTINGS.toString(),
												body, headers, resp);
									}
								}, resp);

					} catch (final Exception e) {
						this.logger.error(e.getMessage(), e);
						resp.end(this.getUtils().getResponse(
								Utils.ERR_502, e.getMessage()).encode());
					}
				}, "listingType", "title");
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
					// Apply role-based query filters
					this.getUtils().assignRoleQueryFilters(xusr, body, false);

					// Add default sorting by creation date
					if (!body.containsKey("sort")) {
						body.put("sort", new JsonObject().put("createdAt", -1));
					}

					this.getDbUtils().find(Collections.LISTINGS.toString(), body, resp);
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

					this.getDbUtils().findOne(Collections.LISTINGS.toString(), query,
							result -> {
								if (result == null || result.isEmpty()) {
									resp.end(this.getUtils().getResponse(
											Utils.ERR_404, "Listing not found").encode());
								} else {
									// Increment view count if not the owner
									if (!result.getString("userId", "").equals(xusr.getString("_id"))) {
										this.incrementListingViews(result.getString("_id"));
									}
									resp.end(this.getUtils().getResponse(result).encode());
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
						// Prepare update data by removing system fields that shouldn't be updated
						JsonObject updateData = body.copy();
						updateData.remove("organizationId")
								.remove("userId")
								.remove("createdAt")
								.remove("views")
								.remove("_id");

						// Add system update timestamp
						updateData.put("updatedAt", Instant.now().toString());

						// Build query with ownership validation
						JsonObject query = new JsonObject()
								.put("_id", body.getString("_id"));

						// Apply role-based filters (users can only update their own listings)
						this.getUtils().assignRoleQueryFilters(xusr, query, false);

						// Additional owner check for non-admin users
						if (!this.getUtils().isRole("admin", xusr) &&
								!this.getUtils().isRole("superadmin", xusr)) {
							query.put("userId", xusr.getString("_id"));
						}

						this.getDbUtils().findOneAndUpdate(Collections.LISTINGS.toString(),
								query, new JsonObject().put("$set", updateData),
								result -> {
									if (result == null || result.isEmpty()) {
										resp.end(this.getUtils().getResponse(
												Utils.ERR_404, "Listing not found or access denied").encode());
									} else {
										resp.end(this.getUtils().getResponse(result).encode());
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

					if (!this.getUtils().isRole("admin", xusr) &&
							!this.getUtils().isRole("superadmin", xusr)) {
						query.put("userId", xusr.getString("_id"));
					}

					JsonObject update = new JsonObject()
							.put("$set", new JsonObject()
									.put("status", "inactive")
									.put("deletedAt", Instant.now().toString())
									.put("deletedBy", xusr.getString("_id")));

					this.getDbUtils().findOneAndUpdate(Collections.LISTINGS.toString(),
							query, update,
							result -> {
								if (result == null || result.isEmpty()) {
									resp.end(this.getUtils().getResponse(
											Utils.ERR_404, "Listing not found or access denied").encode());
								} else {
									resp.end(this.getUtils().getResponse(
											new JsonObject().put("message", "Listing deleted successfully")).encode());
								}
							}, resp);
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
					this.getUtils().assignRoleQueryFilters(xusr, body, false);

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

					this.getDbUtils().find(Collections.LISTINGS.toString(), body, resp);
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
					this.getUtils().assignRoleQueryFilters(xusr, body, false);

					// Default to active types only
					if (!body.containsKey("isActive")) {
						body.put("isActive", true);
					}

					this.getDbUtils().find(Collections.LISTING_TYPES.toString(), body, resp);
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
					JsonObject updateData = body.getJsonObject("update", new JsonObject());
					updateData.put("updatedAt", Instant.now().toString());

					JsonObject query = new JsonObject()
							.put("_id", body.getString("_id"));

					this.getUtils().assignRoleQueryFilters(xusr, query, true);

					this.getDbUtils().findOneAndUpdate(Collections.LISTING_TYPES.toString(),
							query, new JsonObject().put("$set", updateData), resp);
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
									.put("deletedAt", Instant.now().toString()));

					this.getDbUtils().findOneAndUpdate(Collections.LISTING_TYPES.toString(),
							query, update, resp);
				}, "_id");
	}

	/**
	 * Lists listings by specific type.
	 * 
	 * @param rc The routing context.
	 */
	@SystemTasks(task = MODULE + "listListingsByType")
	private void listListingsByType(final RoutingContext rc) {
		this.getUtils().execute2(MODULE + "listListingsByType", rc,
				(xusr, body, params, headers, resp) -> {
					body.put("listingType", body.getString("listingType"));
					body.put("status", "active");

					this.getUtils().assignRoleQueryFilters(xusr, body, false);

					if (!body.containsKey("sort")) {
						body.put("sort", new JsonObject()
								.put("featured", -1)
								.put("createdAt", -1));
					}

					this.getDbUtils().find(Collections.LISTINGS.toString(), body, resp);
				}, "listingType");
	}

	/**
	 * Lists listings by specific user.
	 * 
	 * @param rc The routing context.
	 */
	@SystemTasks(task = MODULE + "listListingsByUser")
	private void listListingsByUser(final RoutingContext rc) {
		this.getUtils().execute2(MODULE + "listListingsByUser", rc,
				(xusr, body, params, headers, resp) -> {
					String targetUserId = body.getString("userId", xusr.getString("_id"));

					// Users can only see their own listings unless they're admin
					if (!this.getUtils().isRole("admin", xusr) &&
							!this.getUtils().isRole("superadmin", xusr)) {
						targetUserId = xusr.getString("_id");
					}

					body.put("userId", targetUserId);
					this.getUtils().assignRoleQueryFilters(xusr, body, false);

					if (!body.containsKey("sort")) {
						body.put("sort", new JsonObject().put("createdAt", -1));
					}

					this.getDbUtils().find(Collections.LISTINGS.toString(), body, resp);
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
					body.put("organizationId", xusr.getString("organisationId"));
					this.getUtils().assignRoleQueryFilters(xusr, body, false);

					if (!body.containsKey("sort")) {
						body.put("sort", new JsonObject()
								.put("featured", -1)
								.put("createdAt", -1));
					}

					this.getDbUtils().find(Collections.LISTINGS.toString(), body, resp);
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
					this.logger.debug("Incremented views for listing: " + listingId);
				},
				error -> {
					// Log error but don't fail the main request
					this.logger.error("Failed to increment views for listing: " + listingId, error);
				});
	}
}
