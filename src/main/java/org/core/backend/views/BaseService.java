package org.core.backend.views;

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
import org.utils.backend.utils.DBUtils;
// import org.utils.backend.utils.KafkaUtils;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

/**
 * The base service.
 */
public class BaseService extends AbstractVerticle {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        BaseService.class.getName());

    /** the double value 100.0. */
    public static final double HUNDRED = 100.0;

    /**
     * Maximum percentage value for discounts.
     */
    public static final int MAX_PERCENTAGE_DISCOUNT = 100;

    /**
     * The double number 0.0.
     */
    public static final double ZERO_DOUBLE  = 0.0;

    /**
     * The db utils instance.
     */
    private DBUtils dbUtils;

    /**
     * The utils instance.
     */
    private Utils utils;

    // /** The kafka utility service. */
    // private KafkaUtils kUtils;

    /**
     * The module name.
     */
    public static final String MODULE = "auth-";

    /**
     * Sets the db utils.
     * @param vertx The vertx instance.
     */
    public void setDBUtils(final Vertx vertx) {
        this.dbUtils = new DBUtils();
    }

    /**
     * Sets the kafka utils.
     * @param vertx The vertx instance
     */
    public void setKafkaUtils(final Vertx vertx) {
        JsonObject config = new JsonObject()
            .put("bootstrap.servers", "localhost:9092")
            .put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer")
            .put("value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer")
            .put("group.id", "your-consumer-group")
            .put("auto.offset.reset", "earliest");
    }

    // /**
    //  * Gets the kafka utils instance.
    //  * @return kUtils The kafka utils instance.
    //  */
    // public KafkaUtils getKafkaUtils() {
    //     return this.kUtils;
    // }


    /**
     * Gets the database utils instance.
     * @return the db utils inatance
     */
    public DBUtils getDbUtils() {
        return this.dbUtils;
    }

    /**
     * Gets the utils instance.
     * @return the utils instance.
     */
    public Utils getUtils() {
        return this.utils;
    }

    /**
     * Sets the genera utils.
     * @param ut The utils instance.
     */
    public void setUtils(final Utils ut) {
        this.utils = ut;
    }

    /**
     * Sets routes for the http server.
     * @param router The router used to set paths.
     */
    protected void setBaseRoutes(final Router router) {
        router.get("/").handler(this::ping);
        router.post("/listTasks").handler(this::listTasks);
    }

    /**
     * Pings the server.
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
     * @param rst The json array result.
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
