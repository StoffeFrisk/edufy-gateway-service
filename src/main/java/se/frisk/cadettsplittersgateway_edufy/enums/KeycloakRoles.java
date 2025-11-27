package se.frisk.cadettsplittersgateway_edufy.enums;

import java.util.Optional;

public enum KeycloakRoles {
    edufy_USER, edufy_ADMIN;

    public static Optional<KeycloakRoles> fromString(String roleName) {
        if (roleName == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(KeycloakRoles.valueOf(roleName));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }


    public String toRoleString() {
        return this.toString();
    }

}
