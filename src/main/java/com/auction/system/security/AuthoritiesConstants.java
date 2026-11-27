package com.auction.system.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";

    public static final String AUCTION_ADMIN = "ROLE_AUCTION_ADMIN";

    public static final String FINANCE_ADMIN = "ROLE_FINANCE_ADMIN";

    public static final String SUPPORT_ADMIN = "ROLE_SUPPORT_ADMIN";

    public static final String SELLER = "ROLE_SELLER";

    public static final String BIDDER = "ROLE_BIDDER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {}
}
