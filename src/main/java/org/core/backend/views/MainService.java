package org.core.backend.views;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.RoutingContext;
import org.core.backend.views.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.AuthUtils;
import org.utils.backend.models.Collections;
import org.utils.backend.utils.IdentityManagement;
import org.utils.backend.utils.SystemTasks;
import org.utils.backend.utils.Utils;

import io.vertx.core.Vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

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
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
            MainService.class.getName());

    /**
     * The sms schedules collections.
     */
    private static final String DB_SCHEDULES = "schedules";

    /**
     * The main vertx microservice callback.
     */
    @Override
    public void start() {
        this.logger.info("Start laxnit-auth Service ->");
        try {
            // Start the http server.
            this.startHttpServer(this.serverPort,
                    new Handler<AsyncResult<HttpServer>>() {

                        @Override
                        public void handle(final AsyncResult<HttpServer> event) {
                            if (event.failed()) {
                                logger.error("Server start failed!",
                                        event.cause());
                            }
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
        }));
        // this.setIdentityManagement(new IdentityManagement(this.getDbUtils()));
        // HttpServerOptions opts = new HttpServerOptions();
        // if (customport < 8000) {
        // this.vertx.createHttpServer()
        // .requestHandler(router)
        // .listen(customport, handler);

        // } else {
        // JksOptions jksOptions = new JksOptions()
        // .setPath(Utils.KEYSTORE_FILE)
        // .setPassword("ThisIsMyPassword23@#");

        // HttpServerOptions opts = new HttpServerOptions()
        // .setSsl(true)
        // .setKeyStoreOptions(jksOptions);

        // this.vertx.createHttpServer(opts)
        // .requestHandler(router)
        // .listen(customport, handler);
        // }

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(customport, handler);

        // Sart blocking processes.
        this.startBlockingProcesses();
        // this.startGrpcServer();
    }

    /**
     * Starts the gRPC server with registered routes.
     */
    private void startGrpcServer() {
        // Create instances of your services
        HelloService helloService = new HelloService(
                this.getUtils(), this.getDbUtils());
        UserService userService = new UserService(
                this.getUtils(), this.getDbUtils());

        VertxServer grpcServer = VertxServerBuilder
                .forAddress(vertx, "0.0.0.0", grpcPort)
                // .addService(new CalculatorService()) // Register gRPC services here
                .addService(helloService.bindService())
                .addService(userService.bindService())
                .intercept(new HeaderInterceptor())
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
                    System.out.println("gRPC server started on port { " + grpcPort + " }");
                } else {
                    System.out.println("gRPC server failed to start -> " + ar.cause());
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
        this.setListingsRoutes(router);
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
     *
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
     * Sets up listings service routes for all authenticated users.
     *
     * @param router The router used to set paths.
     */
    protected void setListingsRoutes(final Router router) {
        this.logger.info("set Listings routes -> ()");

        // Initialize and set up listings service routes
        ListingsService listingsService = new ListingsService();
        listingsService.setDBUtils(this.getDbUtils());
        listingsService.setUtils(this.getUtils());
        listingsService.setListingsRoutes(router);
    }
}