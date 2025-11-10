package se.frisk.cadettsplittersgateway_edufy.services;

import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;
import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;

import java.util.List;

public interface KeycloakService {
    String createUser(KeycloakDTO userDto);
    String getKeycloakUserId(String username);
    void deleteKeycloakUser(String keycloakId);
    void assignRoleToUser(KeycloakDTO userDto);
}
