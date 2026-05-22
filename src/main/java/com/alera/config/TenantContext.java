package com.alera.config;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(String tenant) { CURRENT.set(tenant); }
    public static String getCurrentTenant()            { return CURRENT.get(); }
    public static void clear()                         { CURRENT.remove(); }
}
