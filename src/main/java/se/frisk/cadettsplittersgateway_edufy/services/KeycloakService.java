package se.frisk.cadettsplittersgateway_edufy.services;

import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;
import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;

import java.util.List;

public interface KeycloakService {
    String createUser(KeycloakDTO userDto);
    String getClientAdminToken();
    String getKeycloakUserId(String username);
    void deleteKeycloakUser(KeycloakDTO keycloakDTO);
    void assignRoleToUser(KeycloakDTO userDto);
    void createRealm(String realmName);
    String createClient(String clientId, List<String> redirectUris);
    void createRoles( String clientUuid);
    void keycloakSetup();
}
