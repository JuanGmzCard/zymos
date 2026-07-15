package com.alera.config;

import com.alera.model.Tenant;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.util.WebUtils;

import java.util.Locale;

/**
 * Resuelve el locale en este orden:
 *  1. Cookie "zymos-lang" (preferencia explícita del usuario)
 *  2. Locale del tenant activo (request attribute "currentTenant")
 *  3. Español como fallback
 */
public class TenantAwareCookieLocaleResolver extends CookieLocaleResolver {

    private final String cookieName;

    public TenantAwareCookieLocaleResolver(String cookieName) {
        super(cookieName);
        this.cookieName = cookieName;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // Check if setLocale() already stored a new locale in this request
        // (happens when LocaleChangeInterceptor runs before template rendering).
        // Without this check, the locale change only takes effect on the next request.
        Locale requestLocale = (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
        if (requestLocale != null) {
            return requestLocale;
        }
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            try {
                return Locale.forLanguageTag(cookie.getValue());
            } catch (Exception ignored) {}
        }
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        if (tenant != null && StringUtils.hasText(tenant.getLocale())) {
            return Locale.forLanguageTag(tenant.getLocale());
        }
        return Locale.forLanguageTag("es");
    }
}
