package org.core.backend.views;

import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
// import org.utils.backend.utils.KafkaUtils;

import io.vertx.core.Future;
import org.utils.backend.utils.Utils;

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

        router.post("/testKafka").handler(this::testKafkaProducer);
        // router.post("/testKafkaConsumer")
        //     .handler(this::testKafkaConsumer);

        this.setBaseRoutes(router);
    }

    /**
     * Test kafka.
     * @param rc The routing context
     */
    private void testKafkaProducer(final RoutingContext rc) {

        this.getUtils().send("test2", new JsonObject()
            .put("testing12", "This is a test")
            .put("testing34", "This is a test 2"));
        this.getUtils().send("test", new JsonObject()
            .put("test12", "This is a test")
            .put("test34", "This is a test 2"));
        rc.response().end(this.getUtils()
            .getResponse("Done").encode());

        // this.getKafkaUtils().send("test", new JsonObject()
        //         .put("test", "This is a test")
        //         .put("test", "This is a test 2"))
        //     .onSuccess(meta -> rc.response()
        //         .putHeader("Content-Type", "application/json")
        //         .end(new JsonObject()
        //             .put("status", "sent")
        //             .put("topic", meta.getTopic())
        //             .put("partition", meta.getPartition())
        //             .put("offset", meta.getOffset())
        //             .encodePrettily()))

        //     .onFailure(err -> rc.response()
        //         .setStatusCode(Utils.ERR_500)
        //         .end("Failed to send message: " + err.getMessage()));

    }

    /**
     * Test kafka consumer.
     * @param ctx The routing context
     */
    private void testKafkaConsumer(final RoutingContext ctx) {
        this.getUtils().createConsumer("test")
            .compose(v -> {
                this.getUtils().registerHandler("test", record -> {
                    JsonObject response = new JsonObject()
                        .put("topic", record.topic())
                        .put("key", record.key())
                        .put("message", record.value())
                        .put("partition", record.partition())
                        .put("offset", record.offset());

                    // Log to console – real HTTP pushback
                    // needs WebSocket or SSE.
                    System.out.println("Received message: "
                        + response.encodePrettily());
                });

                return Future.succeededFuture();
            })
            .onSuccess(v -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("status",
                    "consumer registered").encodePrettily()))
            .onFailure(err -> ctx.response()
                .setStatusCode(Utils.ERR_500)
                .end("Failed to consume from topic: " + err.getMessage()));
    }
}
