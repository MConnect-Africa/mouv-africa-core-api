package org.core.backend.views;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;

/**
 * The payments Service.
 */
public class PaymentsService extends BaseService {

    /**
     * The logger instance that is used to log.
     */
    private Logger logger = LoggerFactory.getLogger(
        PaymentsService.class.getName());

    /**
     * Sets routes for the http server.
     *
     * @param router The router used to set paths.
     */
    protected void setPaymentsRoutes(final Router router) {
        this.logger.info("set Payments routes -> ()");

        this.setBaseRoutes(router);
    }
}
