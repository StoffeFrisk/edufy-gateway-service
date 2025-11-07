package se.frisk.cadettsplittersgateway_edufy.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.frisk.cadettsplittersgateway_edufy.dtos.KeycloakDTO;
import se.frisk.cadettsplittersgateway_edufy.enums.KeycloakRoles;
import se.frisk.cadettsplittersgateway_edufy.services.KeycloakServiceImpl;

@RestController
@RequestMapping("/keycloak")


public class KeycloakProxyController {

    private final KeycloakServiceImpl keycloakServiceImpl;

    public KeycloakProxyController(KeycloakServiceImpl keycloakServiceImpl) {
        this.keycloakServiceImpl = keycloakServiceImpl;
    }



    //testing setup
    @GetMapping("/setup")
    public ResponseEntity<String> keycloakSetup(){
        keycloakServiceImpl.keycloakSetup();
        return ResponseEntity.ok("setup complete");
    }



    @PostMapping("/newuser")
    public ResponseEntity<String> newUser(@RequestBody KeycloakDTO userDTO) {
        String createdUserId = keycloakServiceImpl.createUser(userDTO);
        return ResponseEntity.ok(createdUserId);
    }

    @GetMapping("/userid/{username}")
    public ResponseEntity<String> getUserID(@PathVariable String username) {
        return ResponseEntity.ok(keycloakServiceImpl.getKeycloakUserId(username));
    }

    @PutMapping("/assignrole/")
    public ResponseEntity<String> assignRole(@RequestBody KeycloakDTO userDTO) {
        keycloakServiceImpl.assignRoleToUser(userDTO);
        return ResponseEntity.ok("User role set to: " + userDTO.getRole() + ".");
    }


}
