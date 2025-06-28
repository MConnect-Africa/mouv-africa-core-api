package org.core.backend.models;

/**
 * The list of the database collections.
 */
public enum Collections {

    /**
     * The organisation collection.
     */
    ORGANISATION {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "organisation";
        }
    },

    /**
     * The organisation collection.
     */
    USERS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "users";
        }
    },

    /**
     * The rbac tasks collecion.
     */
    RBAC_TASKS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "rbac_task";
        }
    },

    /** document types. */
    DOCUMENT_TYPES {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "documents";
        }
    },

    /** Products. */
    PRODUCTS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "products";
        }
    },

    /** Organisation projects. */
    ORGANISATION_PRODUCTS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "organisation_products";
        }
    },

    /**
     * The listings collection.
     */
    LISTINGS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "listings";
        }
    },

    /**
     * The listing types collection.
     */
    LISTING_TYPES {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "listing_types";
        }
    },


    /**
     * The list of amenities.
     */
    AMENITIES {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "amenities";
        }
    },

    /**
     * The discounts collection.
     */
    DISCOUNTS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "discounts";
        }
    },

    /**
     * The promotions collection.
     */
    PROMOTIONS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "promotions";
        }
    },

    /**
     * The listing promotions collection (many-to-many relationship).
     */
    LISTING_PROMOTIONS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "listing_promotions";
        }
    },

    /**
     * The listing discounts collection (many-to-many relationship).
     */
    LISTING_DISCOUNTS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "listing_discounts";
        }
    },

    /** Bookings. */
    BOOKINGS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "bookings";
        }
    },


    /** reviews. */
    REVIEWS {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "reviews";
        }
    },

    /** reviews. */
    FAVOURITES {
        /**
         * Gets the String version.
         * @return the string ersion
         */
        public String toString() {
            return "favourites";
        }
    }
}
