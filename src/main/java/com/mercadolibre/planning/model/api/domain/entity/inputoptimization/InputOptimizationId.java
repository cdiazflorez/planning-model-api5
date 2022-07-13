package com.mercadolibre.planning.model.api.domain.entity.inputoptimization;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputOptimizationId implements Serializable {

    private static final long serialVersionUID = -8778186008039745895L;

    private String warehouseId;

    private DomainType domain;

}
