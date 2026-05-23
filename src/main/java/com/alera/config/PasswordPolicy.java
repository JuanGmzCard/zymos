package com.alera.config;

/**
 * Política de contraseñas de la aplicación.
 * Requisitos: mínimo 8 caracteres, al menos una letra y al menos un número.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String HINT =
            "Mínimo " + MIN_LENGTH + " caracteres, al menos una letra y un número.";

    private PasswordPolicy() {}

    /**
     * Valida la contraseña según la política.
     * @return null si es válida, o el mensaje de error si no lo es.
     */
    public static String validar(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres.";
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            return "La contraseña debe contener al menos una letra.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "La contraseña debe contener al menos un número.";
        }
        return null;
    }
}
