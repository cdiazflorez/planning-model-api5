package com.mercadolibre.planning.model.api.client.rest.config;

import com.mercadolibre.restclient.MeliRESTPool;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.RESTPool;
import com.mercadolibre.restclient.cache.local.RESTLocalCache;
import com.mercadolibre.restclient.interceptor.AddTimeInterceptor;
import com.mercadolibre.restclient.retry.SimpleRetryStrategy;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties({
        RestClientConfig.RouteEtsClientProperties.class,
        RestClientConfig.RouteCoverageClientProperties.class,
        RestClientConfig.BacklogApiClientProperties.class
})
public class RestClientConfig {
    private RouteEtsClientProperties routeEtsClientProperties;
    private RouteCoverageClientProperties routeCoverageClientProperties;
    private BacklogApiClientProperties backlogApiClientProperties;

    @Bean
    public MeliRestClient restClient() throws IOException {
        return MeliRestClient
                .builder()
                .withPool(
                        restPool(RestPool.ROUTE_ETS.name(), routeEtsClientProperties),
                        restPool(RestPool.ROUTE_COVERAGE.name(), routeCoverageClientProperties),
                        restPool(RestPool.BACKLOG_API.name(), backlogApiClientProperties)
                )
                .build();
    }

    private RESTPool restPool(final String name, final RestClientProperties properties) {
        return restPool(name, properties, null);
    }

    private RESTPool restPool(final String name,
                              final RestClientProperties properties,
                              final RESTLocalCache cache) {

        return MeliRESTPool.builder()
                .withName(name)
                .withBaseURL(properties.getBaseUrl())
                .withConnectionTimeout(properties.getConnectionTimeout())
                .withMaxIdleTime(properties.getMaxIdleTime())
                .withMaxPoolWait(properties.getMaxPoolWait())
                .withRetryStrategy(new SimpleRetryStrategy(
                        properties.getMaxRetries(),
                        properties.getRetriesDelay()))
                .withSocketTimeout(properties.getSocketTimeout())
                .withValidationOnInactivity(properties.getValidationOnInactivity())
                .withWorkerThreads(properties.getWorkerThreads())
                .addInterceptorLast(RestClientLoggingInterceptor.INSTANCE)
                .addInterceptorLast(AddTimeInterceptor.INSTANCE)
                .withCache(cache)
                .build();
    }

    private RESTLocalCache localCache(final String name, final int elements) {
        return new RESTLocalCache(name, elements);
    }

    @ConfigurationProperties("restclient.pool.route-ets")
    public static class RouteEtsClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.route-coverage")
    public static class RouteCoverageClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.backlog")
    public static class BacklogApiClientProperties extends RestClientProperties {
    }
}

