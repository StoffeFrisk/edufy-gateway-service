package se.frisk.cadettsplittersgateway_edufy.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserDTO;
import se.frisk.cadettsplittersgateway_edufy.services.KeycloakServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/keycloak")
public class KeycloakProxyController {

    private final KeycloakServiceImpl keycloakServiceImpl;

    public KeycloakProxyController(KeycloakServiceImpl keycloakServiceImpl) {
        this.keycloakServiceImpl = keycloakServiceImpl;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers() {
        return ResponseEntity.ok(keycloakServiceImpl.getAllKeycloakUsers());
    }

    @PostMapping("/newuser")
    public Mono<ResponseEntity<String>> newUser(@RequestBody UserDTO userDTO) {
        System.out.println("/keycloak/newuser endpoint hit");

        return Mono.fromCallable(() -> keycloakServiceImpl.createUser(userDTO))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/userid/{username}")
    public ResponseEntity<String> getUserID(@PathVariable String username) {
        return ResponseEntity.ok(keycloakServiceImpl.getKeycloakUserId(username));
    }

    @GetMapping("/usernameexists/{username}")
    public ResponseEntity<Boolean> usernameExists(@PathVariable String username) {
        return ResponseEntity.ok(keycloakServiceImpl.keycloakUsernameExists(username));
    }

    @GetMapping("/getuserrole/{username}")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String username) {
        String userId = keycloakServiceImpl.getKeycloakUserId(username);
        return ResponseEntity.ok(keycloakServiceImpl.getUsersRoles(userId));
    }

    @PutMapping("/assignrole")
    public ResponseEntity<String> assignRole(@RequestBody UserDTO userDTO) {
        keycloakServiceImpl.assignRoleToUser(userDTO);
        return ResponseEntity.ok("User role set to: " + userDTO.getRole() + ".");
    }

    @DeleteMapping("/deleteuser/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        keycloakServiceImpl.deleteKeycloakUser(keycloakServiceImpl.getKeycloakUserId(username));
        return ResponseEntity.ok("User " + username + " deleted.");
    }

}
