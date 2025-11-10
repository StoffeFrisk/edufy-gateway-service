package se.frisk.cadettsplittersgateway_edufy.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.frisk.cadettsplittersgateway_edufy.clients.KeycloakClient;
import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;

@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final KeycloakClient keycloakClient;

    @Autowired
    public KeycloakServiceImpl(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @PostConstruct
    public void init() {
        keycloakClient.getAdminToken();
        keycloakClient.keycloakSetup();
    }

    @Override
    public String createUser(KeycloakDTO userDto) {

        String existingUserId = getKeycloakUserId(userDto.getUsername());
        if (existingUserId != null) {
            System.out.println("User '" + userDto.getUsername() + "' already exists with ID: " + existingUserId);
            return existingUserId;
        }

        String existingUserEmail = userDto.getEmail();
        /// add method to check the email is not already in use


        return keycloakClient.createKeycloakUser(userDto);
    }

    @Override
    public String getKeycloakUserId(String username) {
        return keycloakClient.getKeycloakUserId(username);
    }


    @Override
    public void deleteKeycloakUser(String keycloakId) {
        keycloakClient.deleteKeycloakUser(keycloakId);
    }

    @Override
    public void assignRoleToUser(KeycloakDTO userDto) {

        String userId = userDto.getKeycloakId();
        if (userId == null) {
            userId = keycloakClient.getKeycloakUserId(userDto.getUsername());
            if (userId == null) {
                throw new RuntimeException("User not found: " + userDto.getUsername());
            }
            userDto.setKeycloakId(userId);
        }

        keycloakClient.assignRoleToUser(userDto);

    }

}
