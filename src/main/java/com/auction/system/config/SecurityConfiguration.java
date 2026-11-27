package com.auction.system.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.auction.system.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
            String.join(
                "\n",
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.SYSTEM_ADMIN,
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.AUCTION_ADMIN,
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.FINANCE_ADMIN,
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.SUPPORT_ADMIN,
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.SELLER,
                AuthoritiesConstants.SUPER_ADMIN + " > " + AuthoritiesConstants.BIDDER
            )
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/authenticate").permitAll()
                    .requestMatchers("/api/auth/logout").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/admin/auth/activation").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/auth/activate").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auctions", "/api/auctions/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/sellers/*/profile").permitAll()
                    .requestMatchers("/api/register").permitAll()
                    .requestMatchers("/api/activate").permitAll()
                    .requestMatchers("/api/account/reset-password/init").permitAll()
                    .requestMatchers("/api/account/reset-password/finish").permitAll()
                    .requestMatchers("/api/admin/auction/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN, AuthoritiesConstants.AUCTION_ADMIN)
                    .requestMatchers("/api/admin/finance/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN, AuthoritiesConstants.FINANCE_ADMIN)
                    .requestMatchers("/api/admin/support/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN, AuthoritiesConstants.SUPPORT_ADMIN)
                    .requestMatchers("/api/admin/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN)
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/v3/api-docs/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN)
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/**").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/prometheus").permitAll()
                    .requestMatchers("/management/**").hasAnyAuthority(AuthoritiesConstants.SUPER_ADMIN, AuthoritiesConstants.SYSTEM_ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }
}
