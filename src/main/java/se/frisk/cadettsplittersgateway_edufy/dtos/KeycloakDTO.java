package se.frisk.cadettsplittersgateway_edufy.dtos;

import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;

public class KeycloakDTO {
    private String keycloakId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private KeycloakRoles role;

    public KeycloakDTO() {}

    public KeycloakDTO(String keycloakId, String username, String firstName, String lastName, String email, String password, KeycloakRoles role) {
        this.keycloakId = keycloakId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public KeycloakRoles getRole() {
        return role;
    }

    public void setRole(KeycloakRoles role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "KeycloakDTO{" +
                "keycloakId='" + keycloakId + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
