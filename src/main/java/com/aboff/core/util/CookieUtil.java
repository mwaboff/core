package com.aboff.core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing HTTP cookies.
 * Handles creation and deletion of authentication cookies.
 */
@Component
public class CookieUtil {

    private final String cookieName;
    private final boolean httpOnly;
    private final boolean secure;
    private final String sameSite;
    private final int maxAge;

    public CookieUtil(
            @Value("${jwt.cookie.name}") String cookieName,
            @Value("${jwt.cookie.http-only}") boolean httpOnly,
            @Value("${jwt.cookie.secure}") boolean secure,
            @Value("${jwt.cookie.same-site}") String sameSite,
            @Value("${jwt.cookie.max-age}") int maxAge) {
        this.cookieName = cookieName;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = maxAge;
    }

    /**
     * Sets the authentication cookie with the JWT token.
     *
     * @param response the HTTP response to add the cookie to
     * @param token    the JWT token value
     */
    public void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    /**
     * Clears the authentication cookie.
     *
     * @param response the HTTP response to add the clearing cookie to
     */
    public void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    /**
     * Gets the configured cookie name.
     *
     * @return the cookie name
     */
    public String getCookieName() {
        return cookieName;
    }
}
