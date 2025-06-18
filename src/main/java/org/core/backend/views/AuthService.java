package  org.core.backend.views;


import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.models.Collections;
import org.utils.backend.utils.IDBJsonObject;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

/**
 * The authentication service.
 */
public class AuthService extends BookingService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        AuthService.class.getName());

    /**
     * Sets routes for the http server.
     * @param router The router used to set paths.
     */
    protected void setAuthRoutes(final Router router) {
        this.logger.info("set Auth routes -> ()");
        router.post("/createuser").handler(this::createUser);

        router.post("/listusers")
            .handler(this::listUsers);
        router.post("/listroles")
            .handler(this::listRoles);
        router.post("/addroles")
            .handler(this::addRoles);

        router.post("/updateuserdetails")
            .handler(this::updateUserDetails);
        router.post("/updateuserroles")
            .handler(this::updateUserRoles);

        router.post("/getuserdetails")
            .handler(this::getUserDetails);
        router.post("/sendinvite")
            .handler(this::sendInvite);
        router.post("/listinvites")
            .handler(this::listInvites);

        this.setBookingServiceRoutes(router);
    }

    /**
     * Creates the user.
     * @param rc The router used to set paths.
     */
    private void createUser(final RoutingContext rc) {
        this.getUtils().execute3(MODULE + "createUser", rc,
            (xusr, body, params, headers, resp) -> {

                try {
                    String skey = rc.request().getHeader("SKEY");
                    String inviteId = body.getString("inviteId");
                    if (inviteId != null && !inviteId.isEmpty()) {
                        this.getInvitation(body, invite -> {
                            this.getUtils().verifyUniqueUser(
                                skey, body, headers, res -> {
                                    String role = invite.getString("role");
                                    String orgId = invite.getString(
                                        "organisationId");
                                    if (role != null && !role.isEmpty()
                                    && !"client".equalsIgnoreCase(role)) {
                                        this.getUtils().setUserRoles(res, role);
                                    }

                                    if (orgId != null && !orgId.isEmpty()) {
                                        res.put("organisationId", orgId);
                                    }
                                    this.getDbUtils().save(
                                        Collections.USERS.toString(),
                                    res, headers, resp);
                            }, resp);
                        }, resp);
                    } else {
                        this.getUtils().verifyUniqueUser(
                            skey, body, headers, res -> {
                                resp.end(this.getUtils()
                                    .getResponse(res).encode());
                        }, resp);
                    }
                } catch (final Exception e) {
                    this.logger.error(e.getMessage(), e);
                    resp.end(this.getUtils()
                        .getResponse(Utils.ERR_502,
                            e.getMessage()).encode());
                }
        });
    }

    /**
     * Gets the invitation for the invited user.
     * @param body the body from the FE
     * @param success The success callback
     * @param resp The server response.
     */
    private void getInvitation(final JsonObject body,
        final IDBJsonObject success, final HttpServerResponse resp) {
            JsonObject qry = new JsonObject()
                .put("inviteId", body.getString("inviteId"));
            this.getDbUtils().findOne(
                Collections.INVITES.toString(), qry, res -> {
                    if (res == null || res.isEmpty()) {
                        success.run(res);
                    } else {
                        resp.end(this.getUtils().getResponse(
                            Utils.ERR_404, "Invite Not found"
                        ).encode());
                    }
            }, resp);
    }

    /**
     * Confirms the MFA.
     * @param rc The router used to set paths.
     */
    @SystemTasks(task = MODULE + "confirmMultifactor")
    private void confirmMultifactor(final RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        HttpServerResponse resp = rc.response();
        if (this.getUtils().isValid(body, "code", "email")) {
            this.getUtils().confirmResetPasswordCode(body, resp);
        } else {
            resp.end(this.getUtils().getResponse(
                Utils.ERR_502, "Expected fiedls code and email").encode());
        }
    }

    /**
     * list users endpoint.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "listUsers")
    private void listUsers(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listUsers",
            rc, (xusr, body, params, headers, resp) -> {
            this.getUtils().assignRoleQueryFilters(xusr, body, true);
            this.getDbUtils().find(Collections.USERS.toString(),
                body, resp);
        });
    }

    /**
     * lists all user roles in the system.
     * @param rc The router used to set paths.
     */
    @SystemTasks(task = MODULE + "listRoles")
    private void listRoles(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listRoles",
            rc, (xusr, body, params, headers, resp) -> {
                this.getUtils().getRoles(xusr, body, resp);
        });
    }

     /**
     * Creates the user.
     * @param rc The router used to set paths.
     */
    @SystemTasks(task = MODULE + "addRoles")
    private void addRoles(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "addRoles",
            rc, (xusr, body, params, headers, resp) -> {
                this.getUtils().addRoles(xusr, body, () -> {
                    resp.end(this.getUtils().getResponse(body).encode());
                }, fail -> {
                    resp.end(this.getUtils().getResponse(Utils.ERR_502,
                        fail.getMessage()).encode());
                }, resp);
        });
    }


    /**
     * Updates the user details.
     * @param rc The router used to set paths.
     */
    private void updateUserDetails(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateUserDetails",
            rc, (xusr, body, params, headers, resp) -> {
            JsonObject qry = new JsonObject()
                .put("_id", xusr.getString("_id", body.getString("_id")));

            body.remove("feduid");
            body.remove("_id");
            body.remove("token");
            body.remove("roles");
            body.remove("uid");
            body.remove("email");

            this.getDbUtils().findOneAndUpdate(
                Collections.USERS.toString(), qry, body,
                res -> {
                    // send Communication on passord update
                    resp.end(this.getUtils().getResponse(res).encode());
                }, fail -> {
                    resp.end(this.getUtils().getResponse(Utils.ERR_502,
                        fail.getMessage()).encode());
            });
        });
    }


    /**
     * Updates the user roles.
     * @param rc The router used to set paths.
     */
    private void updateUserRoles(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "updateUserRoles",
            rc, (xusr, body, params, headers, resp) -> {

            JsonObject qry = new JsonObject()
                .put("_id", xusr.getString("_id", body.getString("_id")));

            this.getUtils().assignRoleQueryFilters(xusr, body, false);
            this.getDbUtils().findOne(
                Collections.USERS.toString(), qry, res -> {

                if (res == null || res.isEmpty()) {
                    resp.end(this.getUtils().getResponse(Utils.ERR_404,
                        "User not found").encode());
                } else {
                    JsonArray roles = body.getJsonArray("roles") == null
                        ? new JsonArray()
                        : body.getJsonArray("roles");
                    JsonObject set = new JsonObject();
                    JsonObject unset = new JsonObject();

                    for (int i = 0; i < roles.size(); i++) {
                        if (roles.getString(i) != null && !roles.isEmpty()) {
                            String role = roles.getString(i);
                            if ("superadmin".equalsIgnoreCase(role)) {
                                unset.put("organisationId", "");
                            } else {
                                set.put("organisationId",
                                    xusr.getString("_organisationIdid"));
                            }
                            this.getUtils().setUserRoles(res,
                                roles.getString(i));
                        }
                    }

                    JsonArray uroles = res.getJsonArray("roles");
                    set.put("roles", roles);

                    JsonObject update = new JsonObject()
                        .put("$set", set)
                        .put("$unset", unset);

                    this.getDbUtils().update(Collections.USERS.toString(),
                        qry, update, () -> {
                            // send role change communication
                            resp.end(this.getUtils()
                                .getResponse(res).encode());
                    }, resp);
                }
            }, resp);
        }, "roles");
    }

    /**
     * list users endpoint.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "getUserDetails")
    private void getUserDetails(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "getUserDetails",
            rc, (xusr, body, params, headers, resp) -> {

            body.put("uid", xusr.getString("uid"));
            this.getDbUtils().findOne(Collections.USERS.toString(),
                body, resp);
        });
    }

    /**
     * Sends invites to new users.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "sendInvite")
    private void sendInvite(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "sendInvite",
            rc, (xusr, body, params, headers, resp) -> {
            String role = body.getString("role", "");
            this.getUtils().assignRoleSaveFilters(xusr, body);
            this.getUtils().sendInvite(body,
                "superadmin".equalsIgnoreCase(role)
                    ? "client" : role, res -> {
                resp.end(this.getUtils()
                    .getResponse(res).encode());
                String message = "Dear " + body.getString("firstName")
                    + " " + body.getString("lastName") + ","
                    + "\n You have been invited to create an "
                    + "account on Mouv Africa platform "
                    + " platform by " + xusr.getString("username") + "."
                    + "\n Kindly click the link below to "
                    + " finish your account creation "
                    + "{sjdhvjhscv}"
                    + "auth/invites/?inviteID=" + res.getString("inviteId");

                this.getUtils().emailByMailgun(body.getString("email"),
                    "Hurrah !!! You Have Been Invited.", message, false, "");
            }, "opentemplate", fail -> {
                resp.end(this.getUtils()
                    .getResponse(Utils.ERR_503, fail.getMessage()).encode());
            }, resp);

        }, "email", "username", "firstName", "lastName",
        "telephone1", "domain");
    }


    /**
     * Sends invites to new users.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listInvites")
    private void listInvites(final RoutingContext rc) {
        this.getUtils().execute3(MODULE + "listInvites",
            rc, (xusr, body, params, headers, resp) -> {
            JsonObject qry  = new JsonObject()
                .put("inviteId", body.getString("invite_id"));

            this.getDbUtils().findOne(Collections.INVITES.toString(),
                qry, resp);

        }, "invite_id");
    }
}
