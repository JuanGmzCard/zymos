package com.alera.config;

import com.alera.repository.TenantRepository;
import com.alera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TenantFilter tenantFilter(TenantRepository tenantRepo,
                                      @Value("${app.default-subdomain:default}") String defaultSubdomain,
                                      @Value("${app.tenant-cache-ttl-minutes:5}") long ttlMinutes) {
        return new TenantFilter(tenantRepo, defaultSubdomain, ttlMinutes);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public LoginAttemptFilter loginAttemptFilter(LoginAttemptService loginAttemptService) {
        return new LoginAttemptFilter(loginAttemptService);
    }

    @Bean
    public FilterRegistrationBean<LoginAttemptFilter> loginAttemptFilterRegistration(LoginAttemptFilter filter) {
        FilterRegistrationBean<LoginAttemptFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public DaoAuthenticationProvider authProvider(UsuarioService usuarioService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            DaoAuthenticationProvider authProvider,
                                            TenantFilter tenantFilter,
                                            LoginAttemptFilter loginAttemptFilter,
                                            AleraAuthSuccessHandler successHandler,
                                            AleraAuthFailureHandler failureHandler,
                                            AleraAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .addFilterBefore(tenantFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(loginAttemptFilter, SecurityContextHolderFilter.class)
            .authenticationProvider(authProvider)
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session
                .invalidSessionUrl("/login?expired=true")
                .sessionConcurrency(c -> c
                    .maximumSessions(10)
                    .expiredUrl("/login?expired=true")
                )
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()

                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/tipos-cerveza/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,
                        "/guardar", "/actualizar/**", "/eliminar/**",
                        "/duplicar/**").hasRole("ADMIN")
                .requestMatchers("/nuevo", "/editar/**").hasRole("ADMIN")

                .requestMatchers("/facturas/**").hasAnyRole("ADMIN", "FACTURACION")
                .requestMatchers("/proveedores/**").hasAnyRole("ADMIN", "FACTURACION")
                .requestMatchers("/inventario/**").hasAnyRole("ADMIN", "INVENTARIO")
                .requestMatchers("/recetas/**").hasAnyRole("ADMIN", "INVENTARIO")
                .requestMatchers("/equipos/**").hasAnyRole("ADMIN", "EQUIPOS")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }
}
