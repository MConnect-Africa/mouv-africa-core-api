package org.core.backend.views;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.models.Collections;
import org.utils.backend.models.RbacTasks;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;


/**
 * The Rbac servce.
 */
public class RbacService extends PaymentsService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        RbacService.class.getName());

    /**
     * Sets routes for the http server.
     * @param router The router used to set paths.
     */
    protected void setRbacRoutes(final Router router) {
        this.logger.info("set Rbac service routes -> ()");

        router.post("/adminlistrbactasks")
            .handler(this::adminFetchRbacTasks);
        router.post("/adminaddrbactask")
            .handler(this::adminAddNewRbacTask);
        router.post("/adminDeleteRbacTask")
            .handler(this::adminDeleteRbacTask);

        this.setPaymentsRoutes(router);
    }

    /**
     * Fetches the rbac Tasks.
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "adminFetchRbacTasks")
    private void adminFetchRbacTasks(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "adminFetchRbacTasks", rc,
        (xusr, body, params, headers, resp) -> {
            this.getUtils().assignRoleQueryFilters(xusr, body, true);
            this.getDbUtils().find(Collections.RBAC_TASKS.toString(),
                body, resp);
        });
    }

    /**
     * Adds the admin rbac tasks.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "adminAddNewRbacTask")
    private void adminAddNewRbacTask(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "adminAddNewRbacTask", rc,
        (xusr, body, params, headers, resp) -> {
            JsonObject qry = new JsonObject()
                .put("task", body.getString("task", "NA"));

            this.getUtils().assignRoleSaveFilters(
                xusr, body, true);
            JsonArray roles = body.getJsonArray("roles",
                body.getJsonArray("role", new JsonArray()));

            this.getDbUtils().findOne(Collections.RBAC_TASKS.toString(),
                qry, (result) -> {
                    if (result == null || result.isEmpty()) {

                        JsonObject task = new JsonObject()
                            .put("task", body.getString("task"));

                        for (int i = 0; i < roles.size(); i++) {
                            this.getUtils().setUserRoles(task,
                                roles.getJsonObject(i)
                                    .getString("role"));
                        }
                        this.getUtils().assignRoleSaveFilters(xusr, task);
                        this.getDbUtils().save(
                            Collections.RBAC_TASKS.toString(),
                                task, headers, resp);
                    } else {
                        
                        for (int i = 0; i < roles.size(); i++) {
                            this.getUtils().setUserRoles(result,
                                roles.getString(i));
                        }

                        this.getDbUtils().save(
                            Collections.RBAC_TASKS.toString(),
                                result, headers, resp);
                    }

                }, resp);
            }, "task", "roles");
    }


    /**
     * Deletes the rbac task.
     * @param rc The routing context
     */
    private void adminDeleteRbacTask(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "adminDeleteRbacTask", rc,
            (xusr, body, params, headers, resp) -> {
            this.getUtils().assignRoleQueryFilters(xusr, body, true);
            this.getDbUtils().remove(
                Collections.RBAC_TASKS.toString(), body, resp);
        }, "task");
    }
}