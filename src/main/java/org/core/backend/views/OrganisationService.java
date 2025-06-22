package org.core.backend.views;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.UUID;
import org.core.backend.models.Status;
import org.core.backend.models.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.IDBJsonObject;
import org.utils.backend.utils.IDBSuccess;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

/**
 * The organisations sevice.
 */
public class OrganisationService extends AdminService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
            OrganisationService.class.getName());

    /**
     * Sets routes for the http server.
     *
     * @param router The router used to set paths.
     */
    protected void serOrganisationService(final Router router) {
        this.logger.info("set organistion routes -> ()");

        router.post("/createorganisation")
            .handler(this::createOrganisation);
        router.post("/getorganisation")
            .handler(this::getOrganisation);
        router.post("/updateorganisation")
            .handler(this::updateOrganisation);
        router.post("/listorganisations")
            .handler(this::listOrganisations);
        router.post("/listStatuses")
            .handler(this::listStatuses);
        router.post("/cleanUp")
            .handler(this::cleanUp);
        router.post("/approveOrganisation")
            .handler(this::approveOrganisation);
        router.post("/approveUser")
            .handler(this::approveUser);


        this.setAdminRoutes(router);

    }

    /**
     * Create an organisation.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createOrganisation")
    private void createOrganisation(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createOrganisation", rc,
                (xusr, body, params, headers, resp) -> {

                    String orgId = xusr.getString("organisationId");
                    if (orgId == null || orgId.isEmpty()) {
                        String orgID = UUID.randomUUID().toString();
                        String userId = this.getUtils().isRole(
                            "superadmin", xusr)
                                ? body.getString("userId", null)
                                : xusr.getString("_id");

                        this.getUser(userId, founder -> {
                            body.put("organisationId", orgID)
                                .put("isActive", true);
                            this.getUtils().addUserToObject(
                                "founder", founder, body);

                            this.getUtils().setUserRoles(founder, "admin");
                            this.getUtils().setUserRoles(founder,
                                    body.getString("accountType"));

                            body.put("status", Status.PENDING);
                            this.getDbUtils().save(
                                Collections.ORGANISATION.toString(),
                                    body, headers, () -> {
                                        founder.put("organisationId", orgID);
                                        this.getDbUtils().save(
                                            Collections.USERS.toString(),
                                            founder, headers);
                                        resp.end(this.getUtils().getResponse(
                                                body).encode());
                                // create default rbac tasks
                                this.createOrganisationDefaultRbacTasks(orgID);
                                    }, fail -> {
                                       this.logger.error(
                                        fail.getMessage(), fail);
                                    resp.end(this.getUtils().getResponse(
                                        Utils.ERR_502,
                                        fail.getMessage()).encode());
                                    });
                        }, () -> {
                            resp.end(this.getUtils().getResponse(
                                    Utils.ERR_503, "User with _id "
                                            + xusr.getString("_id")
                                            + " is missing")
                                    .encode());
                        }, resp);
                    } else {
                        resp.end(this.getUtils().getResponse(
                           Utils.ERR_402,
                           "User Already belongs to an organisation").encode());
                    }
                }, "name", "phoneNumber", "email");
    }

    /**
     * Gets the user record.
     *
     * @param userId  the _id of the user.
     * @param success the success callback method.
     * @param failed  The callback if user is !found
     * @param resp    The server response.
     */
    protected void getUser(final String userId, final IDBJsonObject success,
            final IDBSuccess failed, final HttpServerResponse resp) {
        JsonObject qry = new JsonObject()
                .put("_id", userId);

        if (this.getUtils().isValid(userId)) {
            this.getDbUtils().findOne(
                Collections.USERS.toString(), qry, res -> {
                    if (res == null || res.isEmpty()) {
                        failed.run();
                    } else {
                        success.run(res);
                    }

                }, fail -> {
                    this.logger.error(fail.getMessage(), fail);
                    resp.end(this.getUtils().getResponse(
                            Utils.ERR_502, fail.getMessage()).encode());
                });
        } else {
            failed.run();
        }
    }

    /**
     * Create an organisation.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "getOrganisation")
    private void getOrganisation(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "getOrganisation", rc,
                (xusr, body, params, headers, resp) -> {
                    this.getUtils().assignRoleQueryFilters(xusr, body, true);
                    this.getDbUtils().find(Collections.ORGANISATION.toString(),
                            body, resp);
                });
    }

    /**
     * Update an organisation.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "updateOrganisation")
    private void updateOrganisation(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateOrganisation", rc,
            (xusr, body, params, headers, resp) -> {
                JsonObject qry = new JsonObject()
                    .put("organisationId",
                    xusr.getString("organisationId"));

                this.getDbUtils().findOneAndUpdate(
                        Collections.ORGANISATION.toString(),
                        qry, body.getJsonObject("update"), resp);
            }, "update");
    }

    /**
     * Update an organisation.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "approveOrganisation")
    private void approveOrganisation(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "approveOrganisation", rc,
            (xusr, body, params, headers, resp) -> {
                JsonObject qry = new JsonObject()
                    .put("_id", body.getString("_id"));

                JsonObject update = new JsonObject()
                    .put("status", body.getBoolean("status"))
                    .put("remarks", body.getString("remarks"));

                this.getUtils().addUserToObject(
                    "approvalBy", xusr, update);

                this.getDbUtils().findOneAndUpdate(
                        Collections.ORGANISATION.toString(),
                        qry, update, resp);
            }, "status", "_id", "remarks");
    }


    /**
     * Update an organisation.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "approveUser")
    private void approveUser(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "approveUser", rc,
            (xusr, body, params, headers, resp) -> {
                JsonObject qry = new JsonObject()
                    .put("_id", body.getString("_id"));

                JsonObject update = new JsonObject()
                    .put("status", body.getString("status"))
                    .put("remarks", body.getString("remarks"));

                this.getUtils().addUserToObject(
                    "approvalBy", xusr, update);

                this.getDbUtils().findOneAndUpdate(
                        Collections.USERS.toString(),
                        qry, update, resp);
            }, "status", "_id", "remarks");
    }

    /**
     * Update an organisation.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listOrganisations")
    private void listOrganisations(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listOrganisations", rc,
            (xusr, body, params, headers, resp) -> {
                this.getUtils().assignRoleQueryFilters(xusr, body, true);
                System.out.println(body.encode());
                this.getDbUtils().find(Collections.ORGANISATION.toString(),
                        body, resp);
            });
    }

    /**
     * Update an organisation.
     *
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listStatuses")
    private void listStatuses(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listStatuses", rc,
            (xusr, body, params, headers, resp) -> {
                JsonArray results = new JsonArray();
                for (Status status : Status.values()) {
                    results.add(new JsonObject()
                            .put("name", status.name()));
                }
                resp.end(this.getUtils().getResponse(results).encode());
            });
    }

    /**
     * Creates the organisation default rbac tasks.
     *
     * @param organisationId The organisation id.
     */
    private void createOrganisationDefaultRbacTasks(
            final String organisationId) {
        this.logger.info("createOrganisationDefaultRbacTasks -> ()");
        JsonArray tasks = new JsonArray()
                .add("auth-getUserDetails")
                .add("auth-listRoles")
                .add("auth-listOrganisations")
                .add("auth-getOrganisation")
                .add("auth-updateOrganisation")
                .add("auth-adminAddNewRbacTask")
                .add("auth-adminFetchRbacTasks")
                .add("auth-listUsers")
                .add("auth-addRoles")
                .add("auth-listTasks")
                .add("auth-createListings")
                .add("auth-listListings")
                .add("auth-listListingTypes")
                .add("auth-createListingType")
                .add("auth-listBookings")
                .add("auth-makeABooking")
                .add("auth-listBookings");

        for (int i = Utils.ZERO; i < tasks.size(); i++) {

            JsonArray roles = new JsonArray()
                    .add(new JsonObject()
                            .put("admin", true));

            JsonObject t = new JsonObject()
                    .put("organisationId", organisationId)
                    .put("roles", roles)
                    .put("task", tasks.getString(i));

            JsonObject tas = JsonObject.mapFrom(t);
            tas.remove("_id");
            this.getDbUtils().save(
                    Collections.RBAC_TASKS.toString(), tas, null);
        }
    }

    /**
     * Cleans up records.
     * @param rc The routing context.
     */
    public void cleanUp(final RoutingContext rc) {
         this.getUtils().execute3(MODULE + "listStatuses", rc,
            (xusr, body, params, headers, resp) -> {
                String collection = body.getString("collection");
                body.remove("collection");

                this.getDbUtils().remove(
                    collection, body, resp);
            }, "collection");
    }
}
