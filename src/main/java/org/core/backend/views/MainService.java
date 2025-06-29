package org.core.backend.views;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.RoutingContext;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.models.Collections;
// import org.utils.backend.utils.KafkaUtils;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

import io.vertx.core.Vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
// import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;


/**
 * The premium service.
 */
public class MainService extends AuthService {

    /**
     * Ther server port to listen to.
     */
    private int serverPort = Integer.parseInt(
        System.getenv("PORT"));

    /**
     * The grpc server port to listen to.
     */
    private int grpcPort = Integer.parseInt(
        System.getenv("GRPC_PORT"));

    /**
     * The grpc server port to listen to.
     */
    private int webSocketPort = Integer.parseInt(
        System.getenv("WEBSOCKET_PORT"));

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        MainService.class.getName());

    /**
     * The sms schedules collections.
     */
    private static final String DB_SCHEDULES = "schedules";

    /**
     * The waiting time in milliseconds.
     */
    public static final int WAIT_TIME = 2000;

    /**
     * The main vertx microservice callback.
     */
    @Override
    public void start() {
        this.logger.info("Start laxnit-auth Service ->");
        try {
            // Start the http server.
            this.startHttpServer(this.serverPort, this.webSocketPort,
                event -> {
                    if (event.failed()) {
                        logger.error("Server start failed!",
                                event.cause());
                    }
                }, wsEcent -> {
                    if (wsEcent.failed()) {
                        logger.error("Web socket Server start failed!",
                            wsEcent.cause());
                    }
                });
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }

        this.logger.info("Starting laxnit-auth Service <-");
    }

    /**
     * Starts blocking processes.
     */
    private void startBlockingProcesses() {
        this.vertx.executeBlocking(f -> {
            // blocking processes called here!!
            this.getDbUtils().getDBClient();
            f.complete();
        }, res -> {
            if (res.succeeded()) {
                logger.info("Server started ....");
            } else {
                logger.error(res.cause().getMessage(), res.cause());
            }
        });
    }

    /**
     * Start http server.
     *
     * @param customport A custom server port.
     * @param handler    The result handler.
     */
    protected void startHttpServer(final int customport,
            final Handler<AsyncResult<HttpServer>> handler) {
        this.vertx = Vertx.vertx(new VertxOptions()
                .setMetricsOptions(new MicrometerMetricsOptions()
                        .setPrometheusOptions(new VertxPrometheusOptions()
                                .setEnabled(true))
                        .setEnabled(true)));

        Router router = Router.router(this.vertx);
        this.setRoutes(router);
        this.setDBUtils(this.vertx);
        this.setUtils(new Utils(() -> {
        })); //this.vertx);

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(customport, handler);

        // Sart blocking processes.
        this.startBlockingProcesses();
        // this.startGrpcServer();
    }

    /**
     * Start http server.
     * @param customport A custom server port.
     * @param custowsmport A custom webserver port.
     * @param handler The result handler.
     * @param wshandler The web socket result handler.
     */
    protected void startHttpServer(final int customport,
        final int custowsmport,
        final Handler<AsyncResult<HttpServer>> handler,
        final Handler<AsyncResult<HttpServer>> wshandler) {

        this.vertx = Vertx.vertx(new VertxOptions()
            .setMetricsOptions(new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true))
                .setEnabled(true)));

        Router router = Router.router(this.vertx);
        this.setRoutes(router);
        this.setDBUtils(this.vertx);
        this.setUtils(new Utils(() -> {
        }));
        // this.setKafkaUtils(this.vertx);

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(customport, handler);

        // Sart blocking processes.
        this.startBlockingProcesses();
        // this.startGrpcServer();

        // Health check
        this.setHealthCheck(router);

        // Start web socket processes.
        createWebSocket(custowsmport, null, wshandler);

        // Starts the KAFKA broker.
        // this.startKafkaBroker();

        // Open event bus
        // this.createEventBus();
    }

    /**
     * Create sthe list of consumers.
     * @param topics The topics to send to.
     * @return a future of type CompositeFuture
     */
    private Future<CompositeFuture> createConsumers(final List<String> topics) {
        List<Future> futures = new ArrayList<>();
        for (String topic : topics) {
            futures.add(this.getUtils().createConsumer(topic));
        }
        return CompositeFuture.all(futures);
    }

    /** stsrt the kafka broker. */
    private void startKafkaBroker() {
        List<String> topics = new ArrayList<>();

        topics.add("test");
        topics.add("test2");
        Map<String, Handler<
            KafkaConsumerRecord<String, JsonObject>>> handlers = Map.of(
                "test", this::createTestHandler,
                "test2", this::createTestHandler2
            );

        this.startKafkaBroker(topics,  handlers);
    }

    /**
     * Starts the kafka brokers.
     * @param consumers The list of consumers.
     * @param handlers The handlers fields.
     */
    private void startKafkaBroker(
        final List<String> consumers, final Map<String, Handler<
            KafkaConsumerRecord<String, JsonObject>>> handlers) {

        this.getUtils().initializeProducer()
            .compose(v -> createConsumers(consumers))
            .onSuccess(v -> {
                // Attach handlers per topic
                handlers.forEach((topic, handler) -> {
                    this.getUtils().registerHandler(topic, handler);
                });

                logger.info("Kafka initialized with topics: {}", consumers);
            })
            .onFailure(err -> logger.error("Kafka initialization failed", err));
    }

    /**
     * Crestes the test handler for kafka.
     * @param record The record being read.
     */
    private void createTestHandler(
        final KafkaConsumerRecord<String, JsonObject> record) {
        JsonObject msg = record.value();
        logger.info("Handling test message 1: {}", msg.encodePrettily());

        // Process...
    }

    /**
     * Crestes the test handler for kafka.
     * @param record The record being read.
     */
    private void createTestHandler2(
        final KafkaConsumerRecord<String, JsonObject> record) {
        JsonObject msg = record.value();
        logger.info("Handling test message 2 : {}", msg.encodePrettily());

        // Process...
    }

    /**
     * Sets the system health check.
     * @param router The router used to set paths.
     */
    private void setHealthCheck(final Router router) {
        HealthCheckHandler health = HealthCheckHandler.create(this.vertx);
        health.register("ws", WAIT_TIME, f -> f.complete(Status.OK()));
        health.register("db", WAIT_TIME, f -> {
            if (this.getDbUtils().getDBClient() == null) {
                f.fail("MongoClient (mongoClient) is null!");
            } else {
                this.getDbUtils().getDBClient().find(
                    Collections.USERS.toString(),
                        new JsonObject().put("_id",
                            UUID.randomUUID().toString()), res -> {
                            if (res.succeeded()) {
                                f.complete(Status.OK());
                            } else {
                                f.fail(res.cause().getMessage());
                            }
                        });
            }
        });

        router.get("/health").handler(health);

        HealthCheckHandler healthz = HealthCheckHandler.create(this.vertx);
        healthz.register("ws", WAIT_TIME,
            f -> f.complete(Status.OK()));

        router.get("/healthz").handler(healthz);
    }

    /**
     * Creates a web socket server.
     * @param custowsmport The custom port.
     * @param opts The web server options.
     * @param handler The web socket result handler.
     */
    private void createWebSocket(final int custowsmport,
        final HttpServerOptions opts,
        final Handler<AsyncResult<HttpServer>> handler) {
            if (opts == null) {
                this.vertx.createHttpServer()
                    .webSocketHandler(this::makeWsRequest)
                        .listen(custowsmport, handler);
            } else {
                this.vertx.createHttpServer(opts)
                    .webSocketHandler(this::makeWsRequest)
                        .listen(custowsmport, handler);
            }
    }

    /**
     * Makes web socket requests.
     * @param ws The web socket utils.
     */
    private void makeWsRequest(final ServerWebSocket ws) {
        this.logger.info("makeWsRequest(uri = {"
            + ws.uri()
            + "}, path = {" + ws.path() + "}) ->");
        String path = ws.path() == null
            ? ""
            : ws.path();
        if (path.endsWith("testWebSocket")) {
            this.testWebSocket(ws);
        } else {
            ws.writeTextMessage(getUtils().getResponse(
                Utils.ERR_500, "Unsupported endpoint!").encode());
            ws.close();
        }
    }


    /**
     * Gets the quotes via web socket.
     * @param ws The web socket utils.
     */
    private void testWebSocket(final ServerWebSocket ws) {
        this.getUtils().execute2(MODULE + "testWebSocket", ws,
            (xusr, body, headers, params, resp) -> {
                System.out.println("Def jaaaaaammm ---> ");
                resp.writeTextMessage(
                    getUtils().getResponse(body).encode());

                resp.close();
        });
    }

    /**
     * Starts the gRPC server with registered routes.
     */
    private void startGrpcServer() {
        // Create instances of your services
        /*HelloService helloService = new HelloService(
                this.getUtils(), this.getDbUtils());
        UserService userService = new UserService(
                this.getUtils(), this.getDbUtils());*/

        VertxServer grpcServer = VertxServerBuilder
                .forAddress(vertx, "0.0.0.0", grpcPort)
                // Register gRPC services here
                //.addService(helloService.bindService())
                //.addService(userService.bindService())
                //.intercept(new HeaderInterceptor())
                .build();

        // VertxServerBuilder builder = VertxServerBuilder.forPort(vertx, 8080)
        // .useSsl(options -> options
        // .setSsl(true)
        // .setUseAlpn(true)
        // .setKeyStoreOptions(new JksOptions()
        // .setPath("server-keystore.jks")
        // .setPassword("secret")));
        this.vertx.executeBlocking(future -> {
            grpcServer.start(ar -> {
                if (ar.succeeded()) {
                    System.out.println("gRPC server started on port { "
                        + grpcPort + " }");
                } else {
                    System.out.println("gRPC server failed to start -> "
                        + ar.cause());
                    ar.cause().printStackTrace();
                }
            });
        }, res -> {
            if (res.failed()) {
                logger.error("Blocking operation failed", res.cause());
            }
        });
    }

    /**
     * Closes all the resources that were opened.
     *
     * @param completionHandler The complention handler.
     */
    public void close(final Handler<AsyncResult<Void>> completionHandler) {
        this.vertx.close(completionHandler);
    }

    /**
     * Sets routes for the http server.
     *
     * @param router The router used to set paths.
     */
    private void setRoutes(final Router router) {
        router.route().handler(CorsHandler.create(/* "*" */)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedHeader("SKEY")
            .allowedHeader("ApiKey")
            .allowedHeader("ModuleID")
            .allowedHeader("Authorization")
            .allowedHeader("Access-Control-Allow-Method")
            .allowedHeader("Access-Control-Allow-Origin")
            .allowedHeader("Access-Control-Allow-Credentials")
            .allowedHeader("Content-Type"));

        // Enable multipart form data parsing for all POST API requests.
        router.route().handler(BodyHandler.create());

        router.post("/searchusers").handler(this::searchUsers);
        router.post("/searchorganisations")
            .handler(this::searchOrganisations);
        router.post("/searchsms")
            .handler(this::searchSMS);
        router.post("/searchtemplates")
            .handler(this::searchTemplates);
        this.setAuthRoutes(router);
    }

    /**
     * Searches the database for values input.
     *
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "searchUsers")
    private void searchUsers(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchUsers", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().addFieldsToSearchQuery(body);
                this.getDbUtils().find(
                        Collections.USERS.toString(), body, resp);
            }, "searchTerm", "fieldsToSearchFor");
    }

    /**
     * Searches the database for values input.
     *
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "searchOrganisations")
    private void searchOrganisations(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchOrganisations", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().addFieldsToSearchQuery(body);
                this.getDbUtils().find(
                        Collections.ORGANISATION.toString(), body, resp);
            }, "searchTerm", "fieldsToSearchFor");
    }

    /**
     * Searches the database for values input.
     *
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "searchTemplates")
    private void searchTemplates(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchTemplates", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().addFieldsToSearchQuery(body);
                this.getDbUtils().find(
                        Collections.TEMPLATES.toString(), body, resp);
            }, "searchTerm", "fieldsToSearchFor");
    }

    /**
     * Searches the database for values input.
     * @param rc The routing context
     */
    @SystemTasks(task = MODULE + "searchSMS")
    private void searchSMS(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "searchSMS", rc,
            (xusr, body, params, headers, resp) -> {

                this.getUtils().addFieldsToSearchQuery(body);
                this.getDbUtils().find(
                        DB_SCHEDULES, body, resp);
            }, "searchTerm", "fieldsToSearchFor");
    }

    /**
     * Test the kafka.
     * @param record the record being consumed.
     */
    private void testKafka(final KafkaConsumerRecord<
        String, JsonObject> record) {
        // Your email sending logic
        System.out.print(record.value());

        // Commit offset after processing
        // this.getUtils().commit("user-login")
        //     .onFailure(err -> logger.error("Failed to commit offset", err));
    }
}
