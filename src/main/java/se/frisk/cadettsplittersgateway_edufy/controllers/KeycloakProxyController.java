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
    public Mono<ResponseEntity<List<UserDTO>>> getUsers() {
        return Mono.fromCallable(() -> keycloakServiceImpl.getAllKeycloakUsers())
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/newuser")
    public Mono<ResponseEntity<String>> newUser(@RequestBody UserDTO userDTO) {
        System.out.println("/keycloak/newuser endpoint hit");

        return Mono.fromCallable(() -> keycloakServiceImpl.createUser(userDTO))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/userid/{username}")
    public Mono<ResponseEntity<String>> getUserID(@PathVariable String username) {
        return Mono.fromCallable(() -> keycloakServiceImpl.getKeycloakUserId(username))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/usernameexists/{username}")
    public Mono<ResponseEntity<Boolean>> usernameExists(@PathVariable String username) {
        return Mono.fromCallable(() -> keycloakServiceImpl.keycloakUsernameExists(username))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/getuserrole/{username}")
    public Mono<ResponseEntity<List<String>>> getUserRoles(@PathVariable String username) {
        return Mono.fromCallable(() -> {
                    String userId = keycloakServiceImpl.getKeycloakUserId(username);
                    return keycloakServiceImpl.getUsersRoles(userId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PutMapping("/assignrole")
    public Mono<ResponseEntity<String>> assignRole(@RequestBody UserDTO userDTO) {
        return Mono.fromCallable(() -> {
                    keycloakServiceImpl.assignRoleToUser(userDTO);
                    return "User role set to: " + userDTO.getRole() + ".";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/deleteuser/{username}")
    public Mono<ResponseEntity<String>> deleteUser(@PathVariable String username) {
        return Mono.fromCallable(() -> {
                    String userId = keycloakServiceImpl.getKeycloakUserId(username);
                    keycloakServiceImpl.deleteKeycloakUser(userId);
                    return "User " + username + " deleted.";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

}
