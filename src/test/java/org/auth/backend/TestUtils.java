package org.auth.backend;

import java.net.ServerSocket;
import java.util.Random;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.utils.backend.utils.DBUtils;
import org.utils.backend.utils.IDBFail;
import org.utils.backend.utils.IDBString;
import org.utils.backend.utils.Utils;
import org.utils.backend.utils.exception.InvalidParameterException;

public class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        TestUtils.class.getName());

    public static String errToStr(Exception e){
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName() + ": " + e.getMessage());
        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null){
            for (StackTraceElement s : stack) {
                sb.append("\n " + s.toString());
            }
        }

        return sb.toString();
    }

    public static String errToStr(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName() + ": " + e.getMessage());
        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null){
            for (StackTraceElement s : stack) {
                sb.append("\n " + s.toString());
            }
        }

        return sb.toString();
    }

    public static String getIdToken(final String userName, final String pass) {
        try {
            JsonObject auth = FirebaseTestUtils.auth(userName, pass);
            return auth.getString("idToken");
        } catch (Exception e) {
            return null;
        }
    }

    public static void createUser(
        final String userName, final String pass,
        final IDBString success, final IDBFail fail,
        final JsonObject otherRecords, final String... userRoles) {
            try {
                LOGGER.info("createUser() ->");
                Utils utils = new Utils(() -> { });

                createUser(utils, userName, pass, success, fail,
                    otherRecords, userRoles);
            } catch (Exception e) {
                fail.run(e);
            }
    }

    public static void createUser(
        final Utils utils,
        final String userName, final String pass,
        final IDBString success, final IDBFail fail,
        final JsonObject otherRecords, final String... userRoles) {
            LOGGER.info("createUser*() ->");
        try {
            JsonObject auth = FirebaseTestUtils.auth(userName, pass);
            String idToken = auth.getString("idToken");

            if (otherRecords != null && !otherRecords.isEmpty()) {
                otherRecords.forEach(a -> {
                    auth.put(a.getKey(), a.getValue());
                });
            }

            FirebaseTestUtils.addUserByIdToken(utils, auth, () -> {
                success.run(idToken);
            }, cause -> {

                fail.run(cause);
            }, userRoles);
        } catch (Exception e) {
            fail.run(e);
        }
    }

    /**
     * Gets a free port for use in testing.
     * @return The port.
     */
    public static int getFreePort() {
        int port = -1;
        int retres = 0;
        do {
            try {
                ServerSocket soc = new ServerSocket(getRandomPortInt());
                port = soc.getLocalPort();
                soc.close();
            } catch (Exception e) {
                retres++;
            }

            if (retres == 400) {
                throw new InvalidParameterException("All ports used up.");
            }
        } while (port == -1);

        return port;
    }

    /**
     * Generates a randon number between a port range.
     * @return The random number generated.
     */
    public static int getRandomPortInt() {
        Random r = new Random();
        int low = 8080;
        int high = 8442;
        return r.nextInt(high-low) + low;
    }
}
