package com.mercadolibre.planning.model.api.client.rest.config;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.api.client.rest.config.CacheConfig.CPT_CACHE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
public class CacheTest {

    private static final List<RouteEtsDto> CPT_MOCK_EXPECTED = new ArrayList<>();

    private RouteEtsGateway mock;

    @Autowired
    private RouteEtsGateway routeEtsClient;

    @EnableCaching
    @Configuration
    public static class CachingTestConfig {

        @Bean
        public RouteEtsGateway routeEtsClientMockImplementation() {
            return mock(RouteEtsGateway.class);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CPT_CACHE_NAME);
        }
    }

    @BeforeEach
    public void setUp() {

        CPT_MOCK_EXPECTED.add(new RouteEtsDto("ARBA01_U_351",
                "ARBA01",
                "U",
                "351",
                null,
                null,
                null));

        mock = AopTestUtils.getTargetObject(routeEtsClient);

        reset(mock);

        when(mock.postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build()))
                .thenReturn(CPT_MOCK_EXPECTED)
                .thenThrow(new RuntimeException("Data should be cached"));
    }

    @AfterEach
    public void tearDown() {
        CPT_MOCK_EXPECTED.clear();
    }

    @Test
    public void givenCptFromCache() {

        assertEquals(CPT_MOCK_EXPECTED, routeEtsClient.postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build()));
        verify(mock).postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build());
        assertEquals(CPT_MOCK_EXPECTED, routeEtsClient.postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build()));
        assertEquals(CPT_MOCK_EXPECTED, routeEtsClient.postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build()));
        verify(mock, times(1)).postRoutEts(RouteEtsRequest.builder()
                .fromFilter(List.of(new String[]{"ARBA01"}))
                .build());
        verifyNoMoreInteractions(mock);
    }
}
