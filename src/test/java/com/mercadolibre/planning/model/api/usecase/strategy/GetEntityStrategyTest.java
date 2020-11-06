package com.mercadolibre.planning.model.api.usecase.strategy;

import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetThroughputEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.GetEntityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetEntityStrategyTest {

    @InjectMocks
    private GetEntityStrategy getEntityStrategy;

    @Mock
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @Mock
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @Mock
    private GetThroughputEntityUseCase getThroughputEntityUseCase;

    @BeforeEach
    public void setUp() {
        getEntityStrategy = new GetEntityStrategy(Set.of(
                getHeadcountEntityUseCase, getProductivityEntityUseCase, getThroughputEntityUseCase
        ));
    }

    @Test
    public void testGetByHeadcountOk() {
        // GIVEN
        when(getHeadcountEntityUseCase.supportsEntityType(HEADCOUNT)).thenReturn(true);

        // WHEN - THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputEntityUseCase);
        assertThat(getEntityStrategy.getBy(HEADCOUNT).get())
                .isInstanceOf(GetHeadcountEntityUseCase.class);
    }

    @Test
    public void testGetByProductivityOk() {
        // GIVEN
        when(getProductivityEntityUseCase.supportsEntityType(PRODUCTIVITY)).thenReturn(true);

        // WHEN - THEN
        verifyZeroInteractions(getThroughputEntityUseCase);
        assertThat(getEntityStrategy.getBy(PRODUCTIVITY).get())
                .isInstanceOf(GetProductivityEntityUseCase.class);
    }

    @Test
    public void testGetByThroughputOk() {
        // GIVEN
        when(getThroughputEntityUseCase.supportsEntityType(THROUGHPUT)).thenReturn(true);

        // WHEN - THEN
        assertThat(getEntityStrategy.getBy(THROUGHPUT).get())
                .isInstanceOf(GetThroughputEntityUseCase.class);
    }
}