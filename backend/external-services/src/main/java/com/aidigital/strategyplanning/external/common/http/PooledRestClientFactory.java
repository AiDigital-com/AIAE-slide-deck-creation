package com.aidigital.strategyplanning.external.common.http;

import com.aidigital.strategyplanning.external.common.http.config.PooledHttpClientProperties;
import com.aidigital.strategyplanning.observability.external.ExternalClientMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory that creates {@link RestClient} instances backed by dedicated
 * Apache HttpClient 5 pools. All created clients are closed on container shutdown.
 */
@Component
public class PooledRestClientFactory {

    private final Logbook logbook;
    private final MeterRegistry meterRegistry;
    private final PooledHttpClientProperties properties;
    private final List<ManagedPooledRestClient> managedClients = new ArrayList<>();

    public PooledRestClientFactory(Logbook logbook, MeterRegistry meterRegistry,
                                   PooledHttpClientProperties properties) {
        this.logbook = logbook;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * Creates a {@link RestClient} for the given service, backed by a managed pool.
     *
     * @param name    logical service name (used for diagnostics only)
     * @param baseUrl base URL for all requests made by this client
     * @return a fully configured {@code RestClient}
     */
    public synchronized RestClient createClient(String name, String baseUrl) {
        return createClient(name, baseUrl, properties.getResponseTimeout());
    }

    /**
     * Creates a {@link RestClient} whose response timeout overrides the configured default.
     *
     * <p>Some providers answer far slower than a typical API call — a long AI completion can run
     * for minutes — and one global timeout cannot serve both those calls and ordinary ones. The
     * override is per client, so a slow provider does not relax the ceiling for everything else.
     *
     * @param name            logical service name (used for diagnostics only)
     * @param baseUrl         base URL for all requests made by this client
     * @param responseTimeout maximum wait for the first response byte for this client
     * @return a fully configured {@code RestClient}
     */
    public synchronized RestClient createClient(String name, String baseUrl,
                                                Duration responseTimeout) {
        return buildClient(name, baseUrl, responseTimeout);
    }

    /**
     * Creates a {@link RestClient} with no base URL, for callers that pass an absolute URI per
     * request.
     *
     * <p>Fetching an arbitrary user-supplied website has no fixed host, so a pool keyed by base
     * URL would grow with every distinct site visited. One pool serving absolute URIs is the
     * correct shape instead: Apache HttpClient pools per route inside it, and
     * {@code maxTotalConnections} keeps the total bounded no matter how many hosts appear.
     *
     * @param name            logical service name (used for diagnostics only)
     * @param responseTimeout maximum wait for the first response byte for this client
     * @return a fully configured {@code RestClient} that requires absolute request URIs
     */
    public synchronized RestClient createAbsoluteUrlClient(String name, Duration responseTimeout) {
        return buildClient(name, null, responseTimeout);
    }

    /**
     * Builds a pooled client and registers it for shutdown.
     *
     * @param name            logical service name (used for diagnostics only)
     * @param baseUrl         base URL for all requests, or null when callers pass absolute URIs
     * @param responseTimeout maximum wait for the first response byte for this client
     * @return a fully configured {@code RestClient}
     */
    RestClient buildClient(String name, String baseUrl, Duration responseTimeout) {
        PoolingHttpClientConnectionManager connectionManager = buildConnectionManager();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeout().toMillis()))
            .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout.toMillis()))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(
                properties.getConnectionRequestTimeout().toMillis()))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(TimeValue.ofMilliseconds(
                properties.getIdleEvictionDuration().toMillis()))
            .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(requestFactory)
            .requestInterceptor(new ExternalClientMetricsInterceptor(name, meterRegistry))
            .requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook));
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        RestClient restClient = builder.build();

        ManagedPooledRestClient managed =
            new ManagedPooledRestClient(name, restClient, httpClient, connectionManager);
        managedClients.add(managed);
        return restClient;
    }

    /**
     * Builds the connection manager with pool limits and keep-alive from properties.
     *
     * @return configured {@link PoolingHttpClientConnectionManager}
     */
    public PoolingHttpClientConnectionManager buildConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxConnectionsPerRoute());
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setTimeToLive(TimeValue.ofMilliseconds(properties.getKeepAliveDuration().toMillis()))
            .setValidateAfterInactivity(TimeValue.ofMilliseconds(
                properties.getIdleEvictionDuration().toMillis()))
            .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        return connectionManager;
    }

    /**
     * Closes every pooled client created by this factory.
     */
    @PreDestroy
    public synchronized void destroy() {
        for (ManagedPooledRestClient managed : managedClients) {
            managed.close();
        }
        managedClients.clear();
    }
}
