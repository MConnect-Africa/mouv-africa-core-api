package org.core.backend.views;

import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
// import org.utils.backend.utils.KafkaUtils;

/**
 * The payments Service.
 */
public class PaymentsService extends BaseService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        PaymentsService.class.getName());

    /**
     * Sets routes for the http server.
     *
     * @param router The router used to set paths.
     */
    protected void setPaymentsRoutes(final Router router) {
        this.logger.info("set Payments routes -> ()");

        router.post("/testKafka").handler(this::testKafka);
        this.setBaseRoutes(router);
    }

    /**
     * Test kafka.
     * @param rc The routing context
     */
    private void testKafka(final RoutingContext rc) {

        this.getKafkaUtils().send("test", new JsonObject()
            .put("test", "This is a test"));

    }
}
