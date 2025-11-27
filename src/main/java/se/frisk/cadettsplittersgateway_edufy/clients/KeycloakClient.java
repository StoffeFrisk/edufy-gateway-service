package se.frisk.cadettsplittersgateway_edufy.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserDTO;
import se.frisk.cadettsplittersgateway_edufy.dtos.RoleDTO;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserRepresentation;
import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;
import se.frisk.cadettsplittersgateway_edufy.exceptions.UserAlreadyExistsException;
import se.frisk.cadettsplittersgateway_edufy.exceptions.UserNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakClient {

    private final RestClient restClient;
    private final String kcBootstrapAdminUsername;
    private final String kcBootstrapAdminPassword;
    private final String realmName;
    private final String clientId;
    private String adminToken;
    private Instant tokenExpiry;
    private final List<String> redirectUris = List.of("http://edufy-gateway-service:4505/*");
    private String clientUuid;
    private String clientSecret;
    private String clientToken;
    private static final Set<String> ADMIN_ROLES = Set.of(
            "manage-users",
            "manage-clients",
            "manage-realm",
            "query-users",
            "query-clients",
            "view-realm"
    );

    public KeycloakClient(@Value("${keycloak.kcBootstrapAdminUsername}") String kcBootstrapAdminUsername,
                          @Value("${keycloak.kcBootstrapAdminPassword}") String kcBootstrapAdminPassword,
                          @Value("${keycloak.auth-server-url}") String keycloakUrl,
                          @Value("${keycloak.realm}") String realmName,
                          @Value ("${keycloak.client-id}") String clientId) {
        this.kcBootstrapAdminUsername = kcBootstrapAdminUsername;
        this.kcBootstrapAdminPassword = kcBootstrapAdminPassword;
        this.realmName = realmName;
        this.clientId = clientId;

        this.restClient = RestClient.builder()
                .baseUrl(keycloakUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

    }

    public void addAudienceMapperToClient(String clientUuid) {
        ensureAdminToken();

        Map<String, Object> mapperBody = Map.of(
                "name", "audience-mapper",
                "protocol", "openid-connect",
                "protocolMapper", "oidc-audience-mapper",
                "consentRequired", false,
                "config", Map.of(
                        "included.client.audience", clientId,
                        "id.token.claim", "true",
                        "access.token.claim", "true"
                )
        );

        try{
        restClient.post()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/protocol-mappers/models",
                        Map.of("realm", realmName, "clientUuid", clientUuid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mapperBody)
                .retrieve()
                .toBodilessEntity();

        System.out.println("Added audience mapper for client " + clientId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                System.out.println("Audience mapper already exists, skipping creation.");
                return;
            }
            throw e;
        }
    }


    private void ensureAdminToken() {
        if (adminToken == null || Instant.now().isAfter(tokenExpiry)) {
            getAdminToken();
        }
    }

    private void ensureClientToken(){
        if(clientToken == null || Instant.now().isAfter(tokenExpiry)){
            getClientCredentialsToken();
        }
    }


    public void getAdminToken(){
        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", kcBootstrapAdminUsername);
        form.add("password", kcBootstrapAdminPassword);

        Map<String, Object> response = restClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        this.adminToken = (String) response.get("access_token");
        int expiresIn = (Integer) response.get("expires_in");
        this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 30);

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain Keycloak admin token");
        }

        this.adminToken = (String) response.get("access_token");
    }

    public String getClientCredentialsToken() {
        ensureAdminToken();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realmName)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain client credentials token");
        }
        clientToken = (String) response.get("access_token");

        int expiresIn = ((Number) response.get("expires_in")).intValue();

        this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 10);

        return clientToken;
    }

    public void ensureRealmExists() {
        ensureAdminToken();
        try {
            restClient.get()
                    .uri("/admin/realms/{realm}", realmName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Realm '" + realmName + "' already exists.");
        } catch (HttpClientErrorException.NotFound e) {

            createRealm();
        }
    }


    public void createRealm() {
        ensureAdminToken();

        Map<String, Object> body = Map.of("realm", realmName, "enabled", true);

        restClient.post()
                .uri("/admin/realms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        System.out.println("Realm '" + realmName + "' created successfully.");
    }

    public String createClient() {
        ensureAdminToken();

        List<Map<String, Object>> existingClients = restClient.get()
                .uri("/admin/realms/{realm}/clients?clientId={clientId}", Map.of(
                        "realm", realmName,
                        "clientId", clientId
                ))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (existingClients != null && !existingClients.isEmpty()) {
            String existingUuid = (String) existingClients.getFirst().get("id");
            System.out.println("Client already exists with UUID: " + existingUuid);
            return existingUuid;
        }

        Map<String, Object> body = Map.of(
                "clientId", clientId,
                "enabled", true,
                "publicClient", false,
                "protocol", "openid-connect",
                "redirectUris", redirectUris,
                "webOrigins", List.of("*"),
                "standardFlowEnabled", true,
                "directAccessGrantsEnabled", true,
                "serviceAccountsEnabled", true,
                "implicitFlowEnabled", false
        );

        ResponseEntity<Void> response = restClient.post()
                .uri("/admin/realms/{realm}/clients", realmName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(body)
                .retrieve()
                .toEntity(Void.class);

        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new RuntimeException("Client created but Location header missing");
        }

        String[] parts = location.toString().split("/");
        clientUuid = parts[parts.length - 1];
        return clientUuid;
    }

    public String fetchClientSecret() {
        ensureAdminToken();

        Map<String, Object> response = restClient.get()
                .uri("/admin/realms/{realm}/clients/{id}/client-secret", realmName, clientUuid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("value")) {
            this.clientSecret = (String) response.get("value");
            System.out.println("Client secret: " + this.clientSecret);
            return clientSecret;
        }

        throw new RuntimeException("Failed to fetch client secret for " + clientUuid);
    }

    public void assignAdminRolesToClient(){
        ensureAdminToken();

        Map<String, Object> serviceAccount = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/service-account-user", realmName, clientUuid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        String serviceUserId = (String) serviceAccount.get("id");

        List<Map<String, Object>> realmManagement   = restClient.get()
                .uri("/admin/realms/{realm}/clients?clientId=realm-management",realmName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if(realmManagement == null || realmManagement.isEmpty()){
            throw new RuntimeException("Could not find realm-management client");
        }

        String realmManagementId = (String) realmManagement.getFirst().get("id");

        List<RoleDTO> selectedRoles = getAdminRoles();

        restClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientId}", Map.of(
                        "realm", realmName,
                        "userId", serviceUserId,
                        "clientId", realmManagementId
                ))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(selectedRoles)
                .retrieve()
                .toBodilessEntity();

    }

    public List<RoleDTO> getAdminRoles() {
        ensureAdminToken();


        List<Map<String, Object>> realmManagementClients = restClient.get()
                .uri("/admin/realms/{realm}/clients?clientId=realm-management", realmName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (realmManagementClients == null || realmManagementClients.isEmpty()) {
            throw new RuntimeException("Could not find realm-management client when fetching admin roles");
        }

        String realmManagementId = (String) realmManagementClients.getFirst().get("id");

        RoleDTO[] roleArray = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientId}/roles", Map.of(
                        "realm", realmName,
                        "clientId", realmManagementId
                ))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(RoleDTO[].class);


        return Arrays.stream(roleArray)
                .filter(r -> ADMIN_ROLES.contains(r.getName()))
                .toList();
    }

    public void createRoles(String clientUuid) {
        ensureClientToken();

        List<Map<String, Object>> existingRoles = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles", Map.of(
                        "realm", realmName,
                        "clientUuid", clientUuid
                ))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(List.class);

        Set<String> existingRoleNames = existingRoles.stream()
                .map(r -> (String) r.get("name"))
                .collect(Collectors.toSet());

        if (!existingRoleNames.contains("edufy_ADMIN")) {
            String adminRoleJson = """
                    {
                      "name": "edufy_ADMIN"
                    }
                    """;

            try {
                restClient.post()
                        .uri("/admin/realms/{realm}/clients/{clientUuid}/roles", Map.of("realm", realmName, "clientUuid", clientUuid))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(adminRoleJson)
                        .retrieve()
                        .toBodilessEntity();

            } catch (Exception e) {
                System.err.println("Error creating roles: " + e.getMessage());
            }
            if (!existingRoleNames.contains("edufy_USER")) {
                String userRoleJson = """
                        {
                          "name": "edufy_USER"
                        }
                        """;
                try {
                    restClient.post()
                            .uri("/admin/realms/{realm}/clients/{clientUuid}/roles", Map.of("realm", realmName, "clientUuid", clientUuid))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(userRoleJson)
                            .retrieve()
                            .toBodilessEntity();

                } catch (Exception e) {
                    System.err.println("Error creating roles: " + e.getMessage());
                }
            }
        }
    }

    public String createKeycloakUser(UserDTO userDto) {
        ensureClientToken();


        List<Map<String, Object>> existingUsers = restClient.get()
                .uri("/admin/realms/{realm}/users?username={username}", Map.of(
                        "realm", realmName,
                        "username", userDto.getUsername()
                ))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(List.class);

        if (existingUsers != null && !existingUsers.isEmpty()) {
            Map<String, Object> firstUser = existingUsers.getFirst();
            String existingId = (String) firstUser.get("id");
            System.out.println("User '" + userDto.getUsername() + "' already exists with ID: " + existingId);
            userDto.setKeycloakId(existingId);
            return existingId;
        }

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", userDto.getUsername());
        userBody.put("firstName", userDto.getFirstName());
        userBody.put("lastName", userDto.getLastName());
        userBody.put("email", userDto.getEmail());
        userBody.put("enabled", true);
        userBody.put("emailVerified", true);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", userDto.getPassword());
        credentials.put("temporary", false);
        userBody.put("credentials", List.of(credentials));

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", Map.of("realm", realmName))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(userBody)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Keycloak create user response status: " + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                URI locationUri = response.getHeaders().getLocation();
                if (locationUri == null) {
                    throw new RuntimeException("User created but Location header is missing");
                }
                String[] parts = locationUri.toString().split("/");
                String userId = parts[parts.length - 1];

                userDto.setKeycloakId(userId);
                return userId;
            } else {
                throw new RuntimeException("Failed to create user: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            System.err.println("Keycloak create user FAILED");
            System.err.println("Status: " + ex.getStatusCode());
            System.err.println("Body:   " + ex.getResponseBodyAsString());


            if (ex.getStatusCode() == HttpStatus.CONFLICT) {

                throw new UserAlreadyExistsException("user", "keycloak", userDto.getUsername());
            }

            throw ex;
        }
    }

    public String getKeycloakUserId(String username){
        ensureClientToken();

        UserRepresentation[] users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .build(Map.of("realm", realmName)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(UserRepresentation[].class);

        if (users != null && users.length > 0) {
            return users[0].getId();
        } else {
            throw new UserNotFoundException(username);
        }
    }

    public boolean emailExists(String email) {
        ensureClientToken();

        UserRepresentation[] users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("email", email)
                        .build(Map.of("realm", realmName)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(UserRepresentation[].class);

        return users != null && users.length > 0;
    }

    public UserRepresentation getUserById(String userId) {
        ensureClientToken();

        try {
            return restClient.get()
                    .uri("/admin/realms/{realm}/users/{id}", Map.of(
                            "realm", realmName,
                            "id", userId
                    ))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .retrieve()
                    .body(UserRepresentation.class);
        } catch (RestClientException e) {
            if (e.getMessage().contains("404")) {
                return null;
            }
            throw e;
        }
    }

    public List<UserRepresentation> getAllUsers(){
        ensureClientToken();

        try {
            List<UserRepresentation> users = restClient.get()
                    .uri("/admin/realms/{realm}/users", realmName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserRepresentation>>() {});

            if (users == null || users.isEmpty()) {
                System.out.println("No users found in realm: " + realmName);
                return List.of();
            }

            return users;

        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching users: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        }
    }


    public void assignRoleToUser(UserDTO userDTO) {
        ensureClientToken();

        String roleName = userDTO.getRole().toString();

        Map<String, Object> role = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}",
                        Map.of("realm", realmName, "clientUuid", clientUuid, "roleName", roleName))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(Map.class);

        if (role == null || !role.containsKey("id")) {
            throw new RuntimeException("Role not found: " + roleName);
        }

        String roleId = (String) role.get("id");
        String userId = userDTO.getKeycloakId();

        Map<String, Object> roleRepresentation = Map.of(
                "id", roleId,
                "name", roleName
        );

        restClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                        Map.of("realm", realmName, "userId", userId, "clientUuid", clientUuid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(List.of(roleRepresentation))
                .retrieve()
                .toBodilessEntity();

    }


    public void deleteKeycloakUser(String keycloakUserId){
        ensureClientToken();

        if(getUserById(keycloakUserId) != null){
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{userId}", Map.of(
                            "realm", realmName,
                            "userId", keycloakUserId
                    ))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Deleted user with ID: " + keycloakUserId);
        }

    }

    public void keycloakSetup() {
        ensureAdminToken();

        ensureRealmExists();

        clientUuid = createClient();

        assignAdminRolesToClient();

        addAudienceMapperToClient(clientUuid);

        clientSecret = fetchClientSecret();

        clientToken = getClientCredentialsToken();

        createRoles(clientUuid);

        UserDTO lynsey = new UserDTO();
        lynsey.setUsername("lynsey");
        lynsey.setFirstName("Lynsey");
        lynsey.setLastName("Fox");
        lynsey.setEmail("lynsey@fox.com");
        lynsey.setPassword("LF123!");
        lynsey.setRole(KeycloakRoles.edufy_USER);

        UserDTO benjamin = new UserDTO();
        benjamin.setUsername("benjamin");
        benjamin.setFirstName("Benjamin");
        benjamin.setLastName("Portsmouth");
        benjamin.setEmail("benjamin@portsmouth.com");
        benjamin.setPassword("BP123!");
        benjamin.setRole(KeycloakRoles.edufy_USER);

        UserDTO christoffer = new UserDTO();
        christoffer.setUsername("christoffer");
        christoffer.setFirstName("Christoffer");
        christoffer.setLastName("Frisk");
        christoffer.setEmail("christoffer@frisk.com");
        christoffer.setPassword("CF123!");
        christoffer.setRole(KeycloakRoles.edufy_USER);

        UserDTO niklas = new UserDTO();
        niklas.setUsername("niklas");
        niklas.setFirstName("Niklas");
        niklas.setLastName("Einarsson");
        niklas.setEmail("niklas@einarsson.com");
        niklas.setPassword("NE123!");
        niklas.setRole(KeycloakRoles.edufy_USER);

        UserDTO admin = new UserDTO();
        admin.setUsername("admin");
        admin.setFirstName("Admin");
        admin.setLastName("Istrator");
        admin.setEmail("administrator@edufy.se");
        admin.setPassword("Admin123!");
        admin.setRole(KeycloakRoles.edufy_ADMIN);

        createKeycloakUser(admin);
        createKeycloakUser(lynsey);
        createKeycloakUser(benjamin);
        createKeycloakUser(christoffer);
        createKeycloakUser(niklas);

        assignRoleToUser(admin);
        assignRoleToUser(lynsey);
        assignRoleToUser(benjamin);
        assignRoleToUser(christoffer);
        assignRoleToUser(niklas);

        System.out.println("Keycloak Realm: edufy-realm successfully set-up with client: edufy-gateway-service and pre-loaded admin and users.");

    }

    public boolean usernameExists(String username) {
        ensureClientToken();

        UserRepresentation[] users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .build(Map.of("realm", realmName)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                .retrieve()
                .body(UserRepresentation[].class);

        return users != null && users.length > 0;
    }

    public List<Map<String,Object>> getUsersRoles(String userId){

        List<Map<String,Object>> roles = restClient.get()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                            Map.of("realm", realmName, "userId", userId, "clientUuid", clientUuid))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .retrieve()
                    .body(List.class);

        if(roles != null && !roles.isEmpty()){
            System.out.println(roles);
        }

        return roles;

}




}
