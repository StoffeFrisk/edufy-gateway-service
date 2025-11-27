package se.frisk.cadettsplittersgateway_edufy.services;

import se.frisk.cadettsplittersgateway_edufy.dtos.UserDTO;

import java.util.List;

public interface KeycloakService {
    String createUser(UserDTO userDto);
    String getKeycloakUserId(String username);
    void deleteKeycloakUser(String keycloakId);
    void assignRoleToUser(UserDTO userDto);
    List<UserDTO> getAllKeycloakUsers();
}
