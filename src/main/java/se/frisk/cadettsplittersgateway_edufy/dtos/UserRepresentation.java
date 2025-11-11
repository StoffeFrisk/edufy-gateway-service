package se.frisk.cadettsplittersgateway_edufy.dtos;

import java.util.List;
import java.util.Map;

public class UserRepresentation {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean enabled;
    private boolean emailVerified;
    private List<Map<String, Object>> credentials; // optional

    public UserRepresentation() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public List<Map<String, Object>> getCredentials() { return credentials; }
    public void setCredentials(List<Map<String, Object>> credentials) { this.credentials = credentials; }
}

