package com.alera;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DockerClientImpl;
import org.testcontainers.shaded.com.github.dockerjava.core.RemoteApiVersion;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Workaround para Docker Desktop 4.74+ en Windows con WSL2.
 *
 * Raíz del problema:
 *   - Testcontainers 1.20.6 llama getDockerClient() → getClientForConfig() que hardcodea
 *     VERSION_1_32 → docker-java llama /v1.32/info
 *   - Docker Desktop 4.74 devuelve HTTP 400 con ServerVersion vacío para API < 1.40
 *     en el endpoint /info desde procesos Windows, causando BadRequestException.
 *
 * Solución: sobreescribir test() y getDockerClient() para usar VERSION_1_40 vía TCP.
 *
 * Activada via ~/.testcontainers.properties:
 *   docker.client.strategy=com.alera.WindowsDockerStrategy
 */
public class WindowsDockerStrategy extends DockerClientProviderStrategy {

    private static final String DOCKER_HOST_TCP = "tcp://127.0.0.1:2375";
    private static final String VALIDATION_URL  = "http://127.0.0.1:2375/v1.40/info";

    private volatile DockerClient cachedClient;

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder()
                .dockerHost(URI.create(DOCKER_HOST_TCP))
                .build();
    }

    @Override
    protected boolean test() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(VALIDATION_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sobreescribe getDockerClient() — el método real que usa la validación interna de
     * Testcontainers. getClient() delega a getDockerClient(), por lo que ambos quedan cubiertos.
     * Usa API 1.40 para evitar el HTTP 400 que devuelve Docker Desktop 4.74 para versiones < 1.40.
     */
    @Override
    public DockerClient getDockerClient() {
        if (cachedClient == null) {
            synchronized (this) {
                if (cachedClient == null) {
                    ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                            .dockerHost(URI.create(DOCKER_HOST_TCP))
                            .build();

                    DefaultDockerClientConfig config = DefaultDockerClientConfig
                            .createDefaultConfigBuilder()
                            .withDockerHost(DOCKER_HOST_TCP)
                            .withApiVersion(RemoteApiVersion.VERSION_1_40)
                            .build();

                    cachedClient = DockerClientImpl.getInstance(config, httpClient);
                }
            }
        }
        return cachedClient;
    }

    @Override
    protected int getPriority() {
        return 200;
    }

    @Override
    public String getDescription() {
        return "Docker Desktop Windows TCP 1.40 (WindowsDockerStrategy)";
    }

    @Override
    public boolean isApplicable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}