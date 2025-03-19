package org.core.backend.views;

import io.grpc.stub.StreamObserver;
import io.vertx.core.json.JsonObject;

import org.core.backend.HelloServiceGrpc.HelloServiceImplBase;
import org.core.backend.models.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.DBUtils;
import org.utils.backend.utils.Utils;
import org.core.backend.HelloRequest;
import org.core.backend.HelloResponse;

/**
 * The HelloService handles greeting requests.
 */
public class HelloService extends HelloServiceImplBase {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        AuthService.class.getName());

    /**
     * The utils instance.
     */
    private Utils utils;


    /**
     * The database utils instance.
     */
    private DBUtils dbutils;

    /**
     * The constructor.
     * @param ut The utils instance.
     * @param dbUt The database utils instance
     */
    public HelloService(final Utils ut, final DBUtils dbUt) {
        super();
        this.utils = ut;
        this.dbutils = dbUt;
    }


    /**
     * Implements the sayHello RPC
     * @param request The incoming request containing the user's name
     * @param responseObserver The response observer to send data back to the client
     */
    @Override
    public void sayHello(final HelloRequest request,
        final StreamObserver<HelloResponse> responseObserver) {
        // Extract the name from the request
        String name = request.getName();

        // Create a greeting message
        String message = "Hello, " + name + "! 👋 Welcome to our service.";

        // Prepare the response
        this.getDbUtils().find(Collections.USERS.toString(), new JsonObject(), res -> {
            System.out.println("Got here in with records + " + res.size());
            // Send the response to the client
            HelloResponse response = HelloResponse.newBuilder()
                .setMessage(new JsonObject().put("user", res).encode())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, fail -> {
            System.out.println(fail.getMessage());
            HelloResponse response = HelloResponse.newBuilder()
                .setMessage(fail.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        });
    }

    /**
     * Gets the Database utils instance.
     * @return dbtils instance.
     */
    private DBUtils getDbUtils() {
        return this.dbutils;
    }
}