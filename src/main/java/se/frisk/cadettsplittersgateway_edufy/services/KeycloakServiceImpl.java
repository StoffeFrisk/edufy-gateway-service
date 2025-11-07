package se.frisk.cadettsplittersgateway_edufy.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserRepresentation;
import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final RestTemplate restTemplate;

    private final String keycloakUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String kcBootstrapAdminUsername;
    private final String kcBootstrapAdminPassword;
    private final String newClientId = "edufy-gateway-service";
    private String adminToken;
    private String clientToken;
    private String clientUuid;

    @Autowired
    public KeycloakServiceImpl(
            RestTemplate restTemplate,
            @Value("${keycloak.auth-server-url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.kcBootstrapAdminUsername}") String kcBootstrapAdminUsername,
            @Value("${keycloak.kcBootstrapAdminPassword}") String kcBootstrapAdminPassword
    ) {
        this.restTemplate = restTemplate;
        this.keycloakUrl = keycloakUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.kcBootstrapAdminUsername = kcBootstrapAdminUsername;
        this.kcBootstrapAdminPassword = kcBootstrapAdminPassword;
    }

    @PostConstruct
    public void init() {
        this.adminToken = keycloakAdminSetup();
    }

    public String keycloakAdminSetup(){
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", "BSadmin");
        form.add("password", "BSadmin");

        HttpEntity<MultiValueMap<String,String>> req = new HttpEntity<>(form, headers);

        Map<String,Object> resp = restTemplate.postForObject(tokenUrl, req, Map.class);

        if (resp == null || !resp.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain Keycloak admin token: " + resp);
        }
        return (String) resp.get("access_token");

    }
    @Override
    public void keycloakSetup() {

        ensureRealmExists(realm);

        List<String> redirectUris = List.of("http://localhost:4505/*");

        clientUuid = createClient(newClientId, redirectUris);

        createRoles(clientUuid);

        KeycloakDTO lynsey = new KeycloakDTO();
        lynsey.setUsername("lynsey");
        lynsey.setFirstName("Lynsey");
        lynsey.setLastName("Fox");
        lynsey.setEmail("lynsey@fox.com");
        lynsey.setPassword("LFox123!");
        lynsey.setRole(KeycloakRoles.edufy_USER);

        KeycloakDTO admin = new KeycloakDTO();
        admin.setUsername("admin");
        admin.setFirstName("Admin");
        admin.setLastName("Istrator");
        admin.setEmail("administrator@edufy.se");
        admin.setPassword("Admin123!");
        admin.setRole(KeycloakRoles.edufy_ADMIN);

        createUser(admin);
        createUser(lynsey);

        assignRoleToUser(admin);
        assignRoleToUser(lynsey);


    }


    @Override
    public String createUser(KeycloakDTO userDto) {

        String existingUserId = getKeycloakUserId(userDto.getUsername());
        if (existingUserId != null) {
            System.out.println("User '" + userDto.getUsername() + "' already exists with ID: " + existingUserId);
            return existingUserId;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", userDto.getUsername());
        userBody.put("firstName", userDto.getFirstName());
        userBody.put("lastName", userDto.getLastName());
        userBody.put("email", userDto.getEmail());
        userBody.put("enabled", true);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", userDto.getPassword());
        credentials.put("temporary", false);
        userBody.put("credentials", List.of(credentials));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userBody, headers);

        String url = keycloakUrl + "/admin/realms/" + realm + "/users";

        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            URI locationUri = response.getHeaders().getLocation();
            if (locationUri == null) {
                throw new RuntimeException("User created but Location header is missing");
            }
            String[] parts = locationUri.toString().split("/");
            String userId = parts[parts.length - 1];
            System.out.println("Created user '" + userDto.getUsername() + "' with ID: " + userId);
            userDto.setKeycloakId(userId);
            return userId;
        } else {
            throw new RuntimeException("Failed to create user: " + response.getStatusCode());
        }
    }


    @Override
    public String getClientAdminToken() {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", newClientId);
        form.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        Map<String, Object> response = restTemplate.postForObject(tokenEndpoint, request, Map.class);


        if (response != null && response.containsKey("access_token")) {
            return (String) response.get("access_token");
        } else {
            throw new RuntimeException("Failed to get admin token from Keycloak");
        }

    }

    @Override
    public String getKeycloakUserId(String username) {

        String url = keycloakUrl + "/admin/realms/" + realm + "/users?username=" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<UserRepresentation[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                UserRepresentation[].class
        );

        UserRepresentation[] users = response.getBody();
        if (users != null && users.length > 0) {
            return users[0].getId();
        } else {
            return null;
        }
    }



    @Override
    public void deleteKeycloakUser(KeycloakDTO keycloakDTO) {

    }

    @Override
    public void assignRoleToUser(KeycloakDTO userDto) {

        String userId = userDto.getKeycloakId();
        if (userId == null) {
            userId = getKeycloakUserId(userDto.getUsername());
            if (userId == null) {
                throw new RuntimeException("User not found: " + userDto.getUsername());
            }
            userDto.setKeycloakId(userId);
        }

        String roleName = userDto.getRole().toString();
        String roleUrl = String.format("%s/admin/realms/%s/clients/%s/roles/%s",
                keycloakUrl, realm, clientUuid, roleName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> roleRequest = new HttpEntity<>(headers);

        Map<String, Object> role = restTemplate.exchange(roleUrl, HttpMethod.GET, roleRequest, Map.class).getBody();
        if (role == null || !role.containsKey("id")) {
            throw new RuntimeException("Role not found: " + roleName);
        }
        String roleId = (String) role.get("id");

        String assignUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/clients/%s",
                keycloakUrl, realm, userId, clientUuid);

        Map<String, Object> roleRepresentation = Map.of(
                "id", roleId,
                "name", roleName
        );

        HttpEntity<List<Map<String, Object>>> assignRequest = new HttpEntity<>(List.of(roleRepresentation), headers);

        restTemplate.postForEntity(assignUrl, assignRequest, Void.class);

        System.out.println("Assigned role '" + roleName + "' to user '" + userDto.getUsername() + "'");
    }


    public String getClientUuid(String clientId) {

        String url = keycloakUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.GET, request, Void.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            URI locationUri = response.getHeaders().getLocation();
            if (locationUri != null) {
                String[] parts = locationUri.toString().split("/");
                return parts[parts.length - 1];
            } else {
                return null;
            }
        } else if (response.getStatusCode().value() == 404) {
            return null;
        } else {
            throw new RuntimeException("Failed to retrieve client UUID: " + response.getStatusCode());
        }
    }


    public void ensureRealmExists(String realmName) {
        String url = keycloakUrl + "/admin/realms/" + realmName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        System.out.println("Using token: " + adminToken); /// test

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            System.out.println("Realm '" + realmName + "' already exists.");
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("Creating realm '" + realmName + "'...");
            createRealm(realm);
        }
    }

    @Override
    public void createRealm(String realmName) {
        String url = keycloakUrl + "/admin/realms";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> body = Map.of(
                "realm", realmName,
                "enabled", true
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, request, String.class);

        System.out.println("Realm '" + realmName + "' created successfully.");
    }


    @Override
    public String createClient(String newClientId, List<String> redirectUris) {

        String existingUuid = getClientUuid(newClientId);
        System.out.println("clientId: " + newClientId); //test
        if (existingUuid != null) {
            System.out.println("Client already exists with UUID: " + existingUuid);
            return existingUuid;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> clientBody = new HashMap<>();
        clientBody.put("clientId", newClientId);
        clientBody.put("enabled", true);
        clientBody.put("publicClient", false);
        clientBody.put("redirectUris", redirectUris);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(clientBody, headers);
        String url = keycloakUrl + "/admin/realms/" + realm + "/clients";

        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            URI locationUri = response.getHeaders().getLocation();
            if (locationUri == null) {
                throw new RuntimeException("Client created but Location header is missing");
            }
            String[] parts = locationUri.toString().split("/");
            String clientUuid = parts[parts.length - 1];
            System.out.println("Created client with UUID: " + clientUuid);
            return clientUuid;
        } else {
            throw new RuntimeException("Failed to create client: " + response.getStatusCode());
        }
    }


    @Override
    public void createRoles(String clientUuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String adminRoleJson = """
                {
                "name": "edufy_ADMIN"
                }
                """;

        String userRoleJson = """
                {
                "name": "edufy_USER"
                }
                """;

        String url = keycloakUrl + "/admin/realms/" + realm + "/clients/%s/roles";

        try {
            restTemplate.exchange(
                    String.format(url, clientUuid),
                    HttpMethod.POST,
                    new HttpEntity<>(adminRoleJson, headers),
                    String.class
            );
            System.out.println("Admin role created");

            restTemplate.exchange(
                    String.format(url, clientUuid),
                    HttpMethod.POST,
                    new HttpEntity<>(userRoleJson, headers),
                    String.class
            );
            System.out.println("User role created");

        } catch (HttpClientErrorException e) {
            System.err.println("Error creating roles: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        }

    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public String getKeycloakUrl() {
        return keycloakUrl;
    }

    public String getRealm() {
        return realm;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
