package com.mercadolibre.planning.model.api.web.controller.ratios.response;

import lombok.Value;

@Value
public class PackingRatio {
  Double packingToteRatio;

  Double packingWallRatio;
}