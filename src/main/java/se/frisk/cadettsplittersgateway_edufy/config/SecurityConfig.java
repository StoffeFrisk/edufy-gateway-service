
package se.frisk.cadettsplittersgateway_edufy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import se.frisk.cadettsplittersgateway_edufy.converters.JwtAuthConverter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;                    //Lynsey Fox

    @Autowired
    public SecurityConfig(final JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;                       //Lynsey Fox
    }


    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->                                   //Lynsey Fox
                        session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->                                  //Lynsey Fox
                        auth
                                .requestMatchers("/keycloak/setup").permitAll()
                              //  .requestMatchers("/keycloak/**").hasRole("edufy_ADMIN")
                                //.requestMatchers("/api/**").hasAnyRole("edufy_ADMIN","edufy_USER")
                                //.anyRequest().authenticated()
                                .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2->                                  //Lynsey Fox
                        oauth2
                                .jwt(jwt->jwt.jwtAuthenticationConverter(jwtAuthConverter))
                );
        return http.build();
    }
}