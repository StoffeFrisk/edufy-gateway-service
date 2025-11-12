package se.frisk.cadettsplittersgateway_edufy.converters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    @Value("${jwt.auth.converter.resource-id.name}")
    private String resourceIdName;
    @Value("${jwt.auth.converter.principle-attribute}")
    private String principleAttribute;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                        jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                        Stream.concat(
                                extractResourceRoles(jwt).stream(),
                                extractRealmRoles(jwt).stream()
                        )
                )
                .collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt));
    }

    private final Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt){
        Collection<String> resourceRoles;
        Map<String, Object> resourceAccess;
        Map<String, Object> resource;
        if (!jwt.hasClaim("resource_access")) return Set.of();

        resourceAccess = jwt.getClaim("resource_access");
        if(!resourceAccess.containsKey(resourceIdName)) return Set.of();

        resource = (Map<String, Object>) resourceAccess.get(resourceIdName);
        if(!resource.containsKey("roles")) return Set.of();

        resourceRoles = (Collection<String>) resource.get("roles");
        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        if (!jwt.hasClaim("realm_access")) return Set.of();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (!realmAccess.containsKey("roles")) return Set.of();

        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }


    private String getPrincipalClaimName(Jwt jwt){
        String claimName = JwtClaimNames.SUB;
        if(principleAttribute !=null && !principleAttribute.isEmpty()){
            claimName = principleAttribute;
        }
        return jwt.getClaim(claimName);
    }
}

