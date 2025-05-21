package org.auth.backend;

import java.util.Date;

import com.google.api.client.util.Data;
import com.google.firebase.auth.FirebaseAuthException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.utils.backend.utils.DBUtils;
import org.utils.backend.utils.IDBCallback;
import org.utils.backend.utils.IDBFail;
import org.utils.backend.utils.IDBSuccess;
import org.utils.backend.utils.Utils;
import org.utils.backend.utils.exception.FirebaseOperationException;

/**
 * Firebase utility class.
 */
public class FirebaseTestUtils {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FirebaseTestUtils.class);

    public static JsonObject auth(final String email, final String pass)
        throws FirebaseOperationException {
            LOGGER.info("auth() ->");
        try {
            // Try authenticating the user with FB.
            // https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=[API_KEY]
            JsonObject body = new JsonObject()
                .put("email", email)
                .put("password", pass)
                .put("returnSecureToken", true);

            MediaType media = MediaType.get("application/json; charset=utf-8");
            RequestBody reqbody = RequestBody.create(body.encode(), media);
            Request request = new Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?"
                        + "key=AIzaSyDAB9Z2T0QDA1-Q6FcVgdKCJFQAmgMoHFY")

                .post(reqbody)
                .build();

            OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {
                String rst = response.body().string();

                /*
                The response is:
                {
                    "kind": "identitytoolkit#VerifyPasswordResponse",
                    "localId": "dWYeUwArKxRBqrOpoHHqS94DhG23",
                    "email": "test123@gmail.com",
                    "displayName": "",
                    "idToken": "eyJhbGciOiJA",
                    "registered": true,
                    "refreshToken": "AEu4IL0dUw0R4Jp7st5lENFLIKwRfIRiWR5YsbXn1u4LL_",
                    "expiresIn": "3600"
                }
                */
                return new JsonObject(rst);
            }
        } catch (Exception e) {
            throw new FirebaseOperationException(e.getMessage(), e);
        }
    }

    /**
     * Authenticates a user and creates the user in the database,
     *      if it does not exit.
     * @param email The user name used in logging in.
     * @param password The secret pass for the user.
     * @throws FirebaseAuthException If authentication with firebase fails.
     */
    public void authUser(final String email, final String password)
        throws FirebaseAuthException {
            LOGGER.info("authUser() ->");
            JsonObject usr = auth(email, password);
            addUser(usr.getString("localId"), usr, "client");
            LOGGER.info("authUser() <-");
    }

    /**
     * Checks if the user already exists, otherwise, saves the user.
     * @param feduid The idemponent user identifier.
     * @param obj The authentication result.
     * @param userRoles The roles that the user should belong to.
     * @throws FirebaseAuthException If authentication with firebase fails.
     */
    public static void addUser(final String feduid,
        final JsonObject obj, String... userRoles)
        throws FirebaseAuthException {
            LOGGER.info("addUser() ->");

        DBUtils db = new DBUtils();

        db.findOne("app_users", new JsonObject()
            .put("feduid", feduid), usr -> {

                if (usr == null || usr.isEmpty()) {
                    obj.remove("expiresIn");
                    obj.remove("refreshToken");
                    obj.remove("idToken");
                    obj.remove("localId");
                    obj.remove("kind");

                    // adds
                    JsonArray roles = new JsonArray();
                    if (userRoles != null) {
                        for (String r : userRoles) {
                            roles.add(new JsonObject()
                                .put(r, true));
                        }
                    }

                    obj.put("name", (new Data()).toString());
                    obj.put("feduid", feduid);
                    obj.put("roles", roles);

                    // Save the record into the database.
                    saveUser(db, obj);
                }
            }, cause -> {
                LOGGER.error(cause.getMessage(), cause);
            });
    }

    /**
     * Checks if the user already exists, otherwise,
     *   saves the user based on idtoken.
     * @param obj The authentication result.
     * @param userRoles The roles that the user should belong to.
     * @throws FirebaseAuthException If authentication with firebase fails.
     */
    public static void addUserByIdToken(
        final Utils utils,
        final JsonObject obj,
        final IDBSuccess sucess,
        final IDBFail fail,
        final String... userRoles)
        throws FirebaseAuthException {
            LOGGER.info("addUserByIdToken() ->");
        String idToken = obj.getString("idToken");
        utils.getUidFromFirebaseIdToken(idToken, null, new IDBCallback<String>() {

            @Override
            public void run(String uid) {
                LOGGER.info("utils.getUidFromFirebaseIdToken(idToken) -> " + uid);

                utils.getDbUtils().findOne("app_users", new JsonObject()
                    .put("feduid", uid), usr -> {

                        if (usr == null || usr.isEmpty()) {
                            obj.remove("expiresIn");
                            obj.remove("refreshToken");
                            obj.remove("idToken");
                            obj.remove("localId");
                            obj.remove("kind");

                            // adds
                            JsonArray roles = new JsonArray();
                            if (userRoles != null) {
                                for (String r : userRoles) {
                                    roles.add(new JsonObject()
                                        .put(r, true));
                                }
                            }

                            obj.put("name", (new Data()).toString());
                            obj.put("feduid", uid);
                            obj.put("roles", roles);

                            // Save the record into the database.
                            saveUser(utils.getDbUtils(), obj, sucess, fail);
                        } else {
                            sucess.run();
                        }
                    }, new IDBFail() {

                        @Override
                        public void run(Throwable cause) {
                            LOGGER.error(cause.getMessage(), cause);
                            fail.run(cause);
                        }
                    });
            }

        }, fail);
    }

        /**
     * Checks if the user already exists, otherwise, saves the user.
     * @param feduid The idemponent user identifier.
     * @param obj The authentication result.
     * @param userRoles The roles that the user should belong to.
     * @throws FirebaseAuthException If authentication with firebase fails.
     */
    public static void addAgencyClient(final String feduid,
        final JsonObject obj)
        throws FirebaseAuthException {

        DBUtils db = new DBUtils();

        db.findOne("agency_clients", new JsonObject()
            .put("feduid", feduid), usr -> {

                if (usr == null || usr.isEmpty()) {
                    obj.remove("expiresIn");
                    obj.remove("refreshToken");
                    obj.remove("idToken");
                    obj.remove("localId");
                    obj.remove("kind");


                    obj.put("name", (new Date()).toString());
                    obj.put("feduid", feduid);
                    // obj.put("roles", roles);

                    // Save the record into the database.
                    // db.save("agency_clients", usr);
                    saveOrganisation(db, obj);
                }
            }, cause -> {
                LOGGER.error(cause.getMessage(), cause);
            });
    }

     /**
     * Save user.
     * @param db Database users.
     * @param usr User to be added.
     */
    public static void saveOrganisation(DBUtils db, JsonObject usr) {
        db.save("organisations", usr, null);
    }

    /**
     * Save user.
     * @param db Database users.
     * @param usr User to be added.
     */
    public static void saveUser(DBUtils db, JsonObject usr,
        IDBSuccess success, IDBFail fail) {
            LOGGER.info("saveUser() ->");
            db.save("users", usr, null, success, fail);
    }

    /**
     * Save user.
     * @param db Database users.
     * @param usr User to be added.
     */
    public static void saveUser(DBUtils db, JsonObject usr) {
        LOGGER.info("saveUser*() ->");
        db.save("users", usr, null);
    }
}