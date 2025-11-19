
package se.frisk.cadettsplittersgateway_edufy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import se.frisk.cadettsplittersgateway_edufy.converters.ReactiveJwtAuthConverter;

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ReactiveJwtAuthConverter jwtAuthConverter;                    //Lynsey Fox

    @Autowired
    public SecurityConfig(final ReactiveJwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;                       //Lynsey Fox
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeExchange(exchanges ->
                        exchanges
                                .pathMatchers("/actuator/**", "/health", "/info").permitAll()
                                .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthConverter)
                        ));
        return http.build();
    }
}