package org.core.backend.models;

/**
 * The list of the database collections.
 */
public enum Collections {

    /**
     * The organisation collection.
     */
    ORGANISATION {
        public String toString() {
            return "organisation";
        }
    },

    /**
     * The organisation collection.
     */
    USERS {
        public String toString() {
            return "users";
        }
    },

    /**
     * The rbac tasks collecion.
     */
    RBAC_TASKS {
        public String toString() {
            return "rbac_task";
        }
    },

    DOCUMENT_TYPES {
        public String toString() {
            return "documents";
        }
    },

    PRODUCTS {
        public String toString() {
            return "products";
        }
    },

    ORGANISATION_PRODUCTS {
        public String toString() {
            return "organisation_products";
        }
    },

    /**
     * The listings collection.
     */
    LISTINGS {
        public String toString() {
            return "listings";
        }
    },

    /**
     * The listing types collection.
     */
    LISTING_TYPES {
        public String toString() {
            return "listing_types";
        }
    }
}