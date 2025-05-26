package org.core.backend.views;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;
import org.utils.backend.utils.SystemTasks;

import org.core.backend.models.Collections;
import org.utils.backend.utils.Utils;

public class AdminService extends RbacService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
            AdminService.class.getName());

    /**
     * Sets routes for the http server.
     *
     * @param router The router used to set paths.
     */
    protected void setAdminRoutes(final Router router) {
        this.logger.info("set Admin routes -> ()");

        router.post("/createDocumentTypes")
                .handler(this::createDocumentTypes);
        router.post("/listDocumentTypes")
                .handler(this::listDocumentTypes);
        router.post("/addNewProducts")
                .handler(this::addNewProducts);
        router.post("/listSideBarServices")
                .handler(this::listSideBarServices);
        router.post("/assignOrganisationNewProducts")
                .handler(this::assignOrganisationNewProducts);

        this.setRbacRoutes(router);
    }

    /**
     * Create the document types.
     * 
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "createDocumentTypes")
    private void createDocumentTypes(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "createDocumentTypes", rc,
                (usr, body, params, headers, resp) -> {

                    this.getDbUtils().save(Collections.DOCUMENT_TYPES.toString(),
                            body, headers, resp);
                }, "name");
    }

    /**
     * Lists the document types.
     * 
     * @param rc The routing context.
     */
    @SystemTasks(task = MODULE + "listDocumentTypes")
    private void listDocumentTypes(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "listDocumentTypes", rc,
                (usr, body, params, headers, resp) -> {

                    this.getDbUtils().find(
                            Collections.DOCUMENT_TYPES.toString(), body, resp);
                });
    }

    /**
     * Adds the new product.
     * 
     * @param rc The rouuting context
     */
    private void addNewProducts(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "addNewProducts", rc,
                (usr, body, params, headers, resp) -> {

                    this.getDbUtils().find(
                            Collections.PRODUCTS.toString(), body, resp);
                }, "name");
    }

    /**
     * Assign Products to an Organisation the new product.
     * 
     * @param rc The rouuting context
     */
    private void assignOrganisationNewProducts(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "assignOrganisationNewProducts", rc,
                (usr, body, params, headers, resp) -> {

                    this.getDbUtils().find(
                            Collections.PRODUCTS.toString(), body, res -> {

                                if (res == null || res.isEmpty()) {
                                    resp.end(this.getUtils().getResponse(
                                            Utils.ERR_404, "Product Not found").encode());
                                } else {
                                    this.assignOrganisationNewProducts(
                                            usr, body, res, resp);
                                }
                            }, resp);
                });
    }

    /**
     * Assigns new products to an organisation.
     * 
     * @param xusr     The current user object
     * @param body     The body from the FE
     * @param products The product
     */
    protected void assignOrganisationNewProducts(final JsonObject xusr,
            final JsonObject body, final List<JsonObject> products,
            final HttpServerResponse resp) {

        try {

            JsonArray res = new JsonArray();
            for (int i = 0; i < products.size(); i++) {
                JsonObject product = products.get(i);
                if (product != null && !product.isEmpty()) {
                    product
                            .put("organisationId", body.getString("organisationId"))
                            .put("isActive", true);

                    product.remove("_id");
                    product.put("_id", body.getString("organisationId", "")
                            + product.getString("name"));

                    this.getDbUtils().save(Collections.ORGANISATION_PRODUCTS.toString(),
                            product, null, () -> {
                                res.add(product);
                            }, resp);
                }
            }

            resp.end(this.getUtils().getResponse(res).encode());
        } catch (final Exception e) {
            resp.end(this.getUtils().getResponse(
                    Utils.ERR_502, e.getMessage()).encode());
            this.logger.error(e.getMessage(), e);
        }
    }

    /**
     * Lists the side bar services for organisations.
     * 
     * @param rc The routng context.
     */
    protected void listSideBarServices(final RoutingContext rc) {
        this.getUtils().execute2(MODULE + "assignOrganisationNewProducts", rc,
                (usr, body, params, headers, resp) -> {

                    body.put("isActive", true);
                    this.getUtils().assignRoleQueryFilters(usr, body, false);

                    this.getDbUtils().distinctWithQuery(
                            Collections.ORGANISATION_PRODUCTS.toString(),
                            "name", String.class.getName(), body, res -> {
                                resp.end(this.getDbUtils().getResponse(res).encode());
                            }, fail -> {
                                resp.end(this.getUtils().getResponse(
                                        Utils.ERR_502, fail.getMessage()).encode());
                                this.logger.error(fail.getMessage(), fail);
                            });
                });
    }
}