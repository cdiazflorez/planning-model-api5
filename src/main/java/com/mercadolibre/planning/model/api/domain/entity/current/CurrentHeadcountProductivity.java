package com.mercadolibre.planning.model.api.domain.entity.current;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import java.time.ZonedDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CurrentHeadcountProductivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private ZonedDateTime date;

    @Enumerated(EnumType.STRING)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    private ProcessName processName;

    private long productivity;

    @Enumerated(EnumType.STRING)
    private MetricUnit productivityMetricUnit;

    private long abilityLevel;

    private boolean isActive;

    private String logisticCenterId;

    @CreationTimestamp
    private ZonedDateTime dateCreated;

    @UpdateTimestamp
    private ZonedDateTime lastUpdated;
}
