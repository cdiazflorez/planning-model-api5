package com.mercadolibre.planning.model.api.web.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsumerMessageDTO(
    @JsonProperty("id")
    String id,
    @JsonProperty("msg")
    ProcessingTimeToUpdate message,
    @JsonProperty("publish_time")
    Long publishTime
) {
  public record ProcessingTimeToUpdate(
      String id,
      @JsonProperty("from")
      String logisticCenterId,
      @JsonProperty("event_type")
      String eventType
  ) {
  }
}
