package se.frisk.cadettsplittersgateway_edufy.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import se.frisk.cadettsplittersgateway_edufy.clients.KeycloakClient;
import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserRepresentation;
import se.frisk.cadettsplittersgateway_edufy.exceptions.UserAlreadyExistsException;
import se.frisk.cadettsplittersgateway_edufy.exceptions.UserNotFoundException;
import se.frisk.cadettsplittersgateway_edufy.utils.DTOConvertor;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final KeycloakClient keycloakClient;

    @Autowired
    public KeycloakServiceImpl(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @PostConstruct
    public void init() throws InterruptedException {
        int retries = 20;
        while (retries-- > 0) {
            try {
                keycloakClient.getAdminToken();
                keycloakClient.keycloakSetup();
                return;
    } catch (ResourceAccessException e) {
                System.out.println("Keycloak not ready yet, retrying in 5s...");
                Thread.sleep(5000);
            }
        }
        throw new IllegalStateException("Unable to connect to Keycloak after multiple attempts");
    }

    @Override
    public String createUser(KeycloakDTO userDto) {

        String existingUserId = getKeycloakUserId(userDto.getUsername());
        if (existingUserId != null) {
            throw new UserAlreadyExistsException("user", "keycloak id", existingUserId);
        }

        String existingUserEmail = userDto.getEmail();
        if (keycloakClient.emailExists(existingUserEmail)){
            throw new UserAlreadyExistsException("user", "email", existingUserEmail);
        }


        return keycloakClient.createKeycloakUser(userDto);
    }

    public boolean keycloakUsernameExists(String username){
        return keycloakClient.usernameExists(username);
    }


    @Override
    public String getKeycloakUserId(String username) {
        String userId = keycloakClient.getKeycloakUserId(username);
        if (userId == null) {
            throw new UserNotFoundException(username);
        } return userId;
    }


    @Override
    public void deleteKeycloakUser(String keycloakId) {
        if(keycloakClient.getUserById(keycloakId) == null) {
            throw new UserNotFoundException(keycloakId);
        }
        keycloakClient.deleteKeycloakUser(keycloakId);
    }

    @Override
    public void assignRoleToUser(KeycloakDTO userDto) {

        String userId = userDto.getKeycloakId();
        if (userId == null) {
            userId = keycloakClient.getKeycloakUserId(userDto.getUsername());
            if (userId == null) {
                throw new UserNotFoundException(userDto.getUsername());
            }
            userDto.setKeycloakId(userId);
        }

        keycloakClient.assignRoleToUser(userDto);

    }

    public List<String> getUsersRoles(String userId){
        List<Map<String,Object>> roles = keycloakClient.getUsersRoles(userId);
        return roles
                .stream()
                .map(r -> (String) r.get("name"))
                .toList();
    }

    @Override
    public List<KeycloakDTO> getAllKeycloakUsers() {
        List<UserRepresentation> users = keycloakClient.getAllUsers();

        return users.stream()
                .map(DTOConvertor::toKeycloakDTO)
                .toList();

    }

}
