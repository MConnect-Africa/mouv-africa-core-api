package org.core.backend.views;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.vertx.core.json.JsonObject;

import org.core.backend.User;
import org.core.backend.UserServiceGrpc.UserServiceImplBase;
import org.core.backend.views.HeaderInterceptor;
import org.core.backend.models.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.backend.utils.DBUtils;
import org.utils.backend.utils.Utils;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;


/**
 * The HelloService handles greeting requests.
 */
public class UserService extends UserServiceImplBase {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        UserService.class.getName());

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
    public UserService(final Utils ut, final DBUtils dbUt) {
        super();
        this.utils = ut;
        this.dbutils = dbUt;
    }

    /**
     * Gets the Database utils instance.
     * @return dbtils instance.
     */
    private DBUtils getDbUtils() {
        return this.dbutils;
    }


    /**
     * Gets the Database utils instance.
     * @return dbtils instance.
     */
    private Utils getUtils() {
        return this.utils;
    }


    /**
     * Converts a JsonObject to a Protobuf message of type T.
     * @param jsonObject The JSON object containing data.
     * @param clazz The Protobuf class type (e.g., User.class).
     * @return A built Protobuf message of type T.
     */
    public static <T extends Message> T buildProtoFromJson(
        final JsonObject jsonObject, final Class<T> clazz) {
        if (jsonObject == null || jsonObject.isEmpty()) {
            throw new IllegalArgumentException("JsonObject cannot be null or empty.");
        }

        try {
            // Obtain the builder dynamically using reflection
            Message.Builder builder = (Message.Builder)
                clazz.getMethod("newBuilder").invoke(null);

            // Merge JSON data into the Protobuf builder
            JsonFormat.parser().ignoringUnknownFields()
                .merge(jsonObject.encode(), builder);

            return (T) builder.build();

        } catch (final InvalidProtocolBufferException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("JSON to Protobuf conversion "
                + "failed for: " + clazz.getSimpleName(), e);
        } catch (final ReflectiveOperationException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to create builder for"
                + " Protobuf class: " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public void getUserDetails(final User request,
        final StreamObserver<User> responseObserver) {
            this.logger.info("getUserDetails -> ()");

        // final String action, final T context,
        // final Metadata h, final IExecute4 exec, final StreamObserver<T> resp,
        // final String... val
        try {
            Metadata metadata = HeaderInterceptor.getMetadataFromContext();
            this.getUtils().execute2("getUserDetails", request, metadata,
                (usrx, body, headers, resp) -> {

                    System.out.println("Def Jaaaammmmm 22222 -> "
                        + usrx.encode());

                    User response = buildProtoFromJson(
                        usrx, User.class);
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
            }, responseObserver);
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            this.getUtils().sendErrorResponse(responseObserver,
                e.getMessage(), Status.INTERNAL);
        }
    }

    // /**
    //  * The test method to get skey.
    //  * @param request The reuqest
    //  * @param responseObserver The observer.
    //  */
    // public void defjam(final User request,
    //     final StreamObserver<User> responseObserver) {

    //     logger.info("getUserDetails -> Received request for user ID: {}",
    //         request.getId());

    //     // Extract headers from the request context
    //     Metadata metadata = HeaderInterceptor.getMetadata();
    //     if (metadata != null) {
    //         String authToken = metadata.get(Metadata.Key.of(
    //             "authorization", Metadata.ASCII_STRING_MARSHALLER));
    //         String requestId = metadata.get(Metadata.Key.of(
    //             "x-request-id", Metadata.ASCII_STRING_MARSHALLER));

    //         logger.info("Received Authorization Token: {}", authToken);
    //         logger.info("Received Request ID: {}", requestId);
    //     }
    // }


    /**
     * The test method to get skey.
     * @param request The request of type T.
     * @param responseObserver The observer.
     */
    public <T extends Message> void defjam(
            final T request, final StreamObserver<T> responseObserver) {

        System.out.println("defjam -> Received request");

        // Extract headers from the request context
        Metadata metadata = HeaderInterceptor.getMetadataFromContext();
        // Replace with actual way of accessing metadata
        System.out.println("defjam -> Received request - 2 ");
        System.out.println(metadata);
        if (metadata != null) {
            System.out.println("defjam -> Received request - 2 ");
            String authToken = metadata.get(Metadata.Key.of(
                "skey", Metadata.ASCII_STRING_MARSHALLER));
            String apiKey = metadata.get(Metadata.Key.of(
                "apiKey", Metadata.ASCII_STRING_MARSHALLER));
            String requestId = metadata.get(Metadata.Key.of(
                "x-request-id", Metadata.ASCII_STRING_MARSHALLER));

            System.out.println("Received Authorization Token: {"
                + authToken + " }");
            System.out.println("Received Request ID: {"
                + requestId + " }");
        }

        // Just an example: Echo the request back (modify as per your need)
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}