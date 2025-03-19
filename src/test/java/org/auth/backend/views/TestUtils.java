package org.core.backend.views;

import org.utils.backend.utils.DBUtils;
import org.utils.backend.utils.IDBString;
import org.utils.backend.utils.IDBSuccess;
import org.utils.backend.utils.VertxTestUtils;

import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {

     /**
     * The logger instance that is used to log.
     */
    Logger logger = LoggerFactory.getLogger(
        TestUtils.class.getName());

    /**
     * Gets User method.
     * @param dbUtils The database utils instance.
     * @param qry Th query object
     * @param token The token passed back.
     */
    public static void getUser(final DBUtils dbUtils,
        final JsonObject qry, final IDBString token) {
            dbUtils.findOne("users", qry, (res) -> {
                System.out.println(res);
                token.run(res.getString("token"));
            }, fail -> {
                System.out.println(fail.getMessage());
                token.run("N/A");
            });
    }

    public static void saveUser(final DBUtils dbUtils,
    final JsonObject qry, final IDBSuccess token) {
        dbUtils.save("users", qry, null, () -> {
            token.run();
        }, fail -> {
            System.out.println(fail.getMessage());
            token.run();
        });
    }

    public static void updateUser(final DBUtils dbUtils,
    final JsonObject qry, final JsonObject update, final IDBSuccess token) {
        dbUtils.update("users", qry, update, () -> {
            token.run();
        }, fail -> {
            System.out.println(fail.getMessage());
            token.run();
        });
    }

    public static void removeUser(final DBUtils dbUtils,
    final JsonObject qry, final IDBSuccess token) {
        dbUtils.remove("users", qry, VertxTestUtils.createHttpResponse(res -> {
            System.out.println("Done ===> ");
        }));
    }
}