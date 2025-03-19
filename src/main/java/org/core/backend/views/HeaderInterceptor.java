package org.core.backend.views;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class HeaderInterceptor implements ServerInterceptor {

    private static final Context.Key<Metadata> METADATA_CONTEXT_KEY = Context.key("metadata");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        // Store metadata in the context
        Context context = Context.current().withValue(METADATA_CONTEXT_KEY, headers);
        return Contexts.interceptCall(context, call, headers, next);
    }

    // Provide a method to retrieve metadata inside service implementations
    public static Metadata getMetadataFromContext() {
        return METADATA_CONTEXT_KEY.get();
    }
}
