package com.alera.config;

import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

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
                                      @Value("${app.tenant-cache-ttl-minutes:5}") long ttlMinutes,
                                      @Value("${app.plan.dias-gracia:7}") int diasGracia) {
        return new TenantFilter(tenantRepo, defaultSubdomain, ttlMinutes, diasGracia);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public CspNonceFilter cspNonceFilter() {
        return new CspNonceFilter();
    }

    @Bean
    public FilterRegistrationBean<CspNonceFilter> cspNonceFilterRegistration(CspNonceFilter filter) {
        FilterRegistrationBean<CspNonceFilter> reg = new FilterRegistrationBean<>(filter);
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
    public ApiRateLimitFilter apiRateLimitFilter(
            @Value("${app.api.rate-limit:100}") int apiLimit,
            @Value("${app.admin.import-rate-limit:10}") int importLimit) {
        return new ApiRateLimitFilter(apiLimit, importLimit);
    }

    @Bean
    public FilterRegistrationBean<ApiRateLimitFilter> apiRateLimitFilterRegistration(ApiRateLimitFilter filter) {
        FilterRegistrationBean<ApiRateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtFilter jwtFilter(JwtService jwtService, UsuarioService usuarioService) {
        return new JwtFilter(jwtService, usuarioService);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOriginsStr) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (allowedOriginsStr == null || allowedOriginsStr.isBlank()) return source;
        List<String> origins = Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
        if (origins.isEmpty()) return source;
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", config);
        return source;
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
                                            CspNonceFilter cspNonceFilter,
                                            TenantFilter tenantFilter,
                                            LoginAttemptFilter loginAttemptFilter,
                                            JwtFilter jwtFilter,
                                            ApiRateLimitFilter apiRateLimitFilter,
                                            ZymosAuthSuccessHandler successHandler,
                                            ZymosAuthFailureHandler failureHandler,
                                            ZymosAccessDeniedHandler accessDeniedHandler,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .addFilterBefore(cspNonceFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(tenantFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(loginAttemptFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
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
                .requestMatchers("/error", "/error/**").permitAll()
                .requestMatchers("/plan-vencido").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()

                .requestMatchers("/admin/tenants/**").hasRole("SUPERADMIN")
                .requestMatchers("/usuarios/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/tipos-cerveza/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")

                .requestMatchers(HttpMethod.POST,
                        "/guardar", "/actualizar/**", "/eliminar/**",
                        "/duplicar/**").hasAnyRole("ADMIN", "SUPERADMIN", "PRODUCCION")
                .requestMatchers("/nuevo", "/editar/**").hasAnyRole("ADMIN", "SUPERADMIN", "PRODUCCION")

                .requestMatchers("/facturas/**").hasAnyRole("ADMIN", "FACTURACION", "SUPERADMIN")
                .requestMatchers("/ventas/**").hasAnyRole("ADMIN", "FACTURACION", "SUPERADMIN")
                .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "FACTURACION", "SUPERADMIN")
                .requestMatchers("/proveedores/**").hasAnyRole("ADMIN", "FACTURACION", "SUPERADMIN")
                .requestMatchers("/inventario/**").hasAnyRole("ADMIN", "INVENTARIO", "PRODUCCION", "SUPERADMIN")
                .requestMatchers("/recetas/**").hasAnyRole("ADMIN", "INVENTARIO", "PRODUCCION", "SUPERADMIN")
                .requestMatchers("/equipos/**").hasAnyRole("ADMIN", "EQUIPOS", "PRODUCCION", "SUPERADMIN")
                .requestMatchers("/barriles/**").hasAnyRole("ADMIN", "INVENTARIO", "PRODUCCION", "SUPERADMIN")
                .requestMatchers("/ordenes-compra/**").hasAnyRole("ADMIN", "FACTURACION", "SUPERADMIN")

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
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .permissionsPolicy(pp -> pp
                    .policy("camera=(), microphone=(), geolocation=(), payment=()")
                )
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/api/auth/**"));
        return http.build();
    }
}
