package org.core.backend.views;


import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;
import org.utils.backend.utils.SystemTasks;

import org.core.backend.models.Collections;

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
        this.setRbacRoutes(router);
    }

    /**
     * Create the document types.
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
}
