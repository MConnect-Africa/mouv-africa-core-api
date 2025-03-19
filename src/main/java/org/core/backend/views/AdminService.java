package org.core.backend.views;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;

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

        this.setRbacRoutes(router);
    }
}