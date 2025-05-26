package org.core.backend.views;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.Method;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.*;

import org.core.backend.models.Collections;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The base service.
 */
public class BaseService extends AbstractVerticle {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
            BaseService.class.getName());

    /**
     * The identity management.
     */
    private IdentityManagement identityManagement;

    private DBUtils dbUtils;

    private AuthUtils authUtils;

    private Utils utils;

    public static final String MODULE = "auth-";

    public void setDBUtils(final Vertx vertx) {
        this.dbUtils = new DBUtils();
    }

    public void setAuthUtils(final AuthUtils authUtils,
            final DBUtils dbUtilsInstance, final Utils utilsInstance) {
        // this.getUtils().setSmsUtils(new SMSUtils(dbUtilsInstance, utilsInstance));
    }

    public DBUtils getDbUtils() {
        return this.dbUtils;
    }

    public Utils getUtils() {
        return this.utils;
    }

    public void setUtils(final Utils ut) {
        this.utils = ut;
    }

    /**
     * Sets the identityManagement.
     * 
     * @param iManagement The identityManagement.
     */
    public void setIdentityManagement(
            final IdentityManagement iManagement) {
        this.identityManagement = iManagement;
    }

    /**
     * Gets the identity management.
     * 
     * @return identityManagement.
     */
    public IdentityManagement getIdentityManagement() {
        return this.identityManagement;
    }

    /**
     * Sets routes for the http server.
     * 
     * @param router The router used to set paths.
     */
    protected void setBaseRoutes(final Router router) {
        router.get("/").handler(this::ping);
        router.post("/listTasks").handler(this::listTasks);
    }

    /**
     * Pings the server.
     * 
     * @param rc The routing context that handles http requests and responses.
     */
    private void ping(final RoutingContext rc) {
        this.logger.info("ping() ->");

        JsonObject serverSetting = new JsonObject()
                .put("Status", "alive")
                .put("version", "0.1.40.2")
                .put("Auto-Gen-ID", UUID.randomUUID().toString());
        rc.response().end(serverSetting.encode());
        this.logger.info("ping() <-");
    }

    /**
     * Lists the tasks.
     * 
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listTasks")
    private void listTasks(final RoutingContext rc) {
        this.getUtils().execute3(MODULE + "listTasks", rc,
                (xusr, body, params, headers, resp) -> {
                    try {
                        resp.end(getUtils().getResponse(
                                listTasks()).encode());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        resp.end(getUtils().getResponse(
                                Utils.ERR_502, e.getMessage()).encode());
                    }
                });
    }

    /**
     * Lists rbac tasks.
     * 
     * @return The json array representing the tasks.
     */
    protected JsonArray listTasks() {
        JsonArray rst = new JsonArray();
        this.addRbacTasks(rst,
                MainService.class.getDeclaredMethods());
        this.addRbacTasks(rst,
                AuthService.class.getDeclaredMethods());
        this.addRbacTasks(rst,
                ListingsService.class.getDeclaredMethods());
        this.addRbacTasks(rst,
                AdminService.class.getDeclaredMethods());
        this.addRbacTasks(rst,
                PaymentsService.class.getDeclaredMethods());
        this.addRbacTasks(rst,
                BaseService.class.getDeclaredMethods());

        return rst;
    }

    /**
     * Adds rbac tasks to the json array.
     * 
     * @param rst     The json array result.
     * @param methods The array of methods.
     */
    private void addRbacTasks(final JsonArray rst, final Method[] methods) {
        if (methods != null && methods.length > Utils.ZERO) {
            for (Method method : methods) {
                SystemTasks task = method.getDeclaredAnnotation(
                        SystemTasks.class);
                if (task != null) {
                    if (task.task() != null
                            && !task.task().trim().isEmpty()) {
                        rst.add(task.task());
                    }
                }
            }
        }
    }
}