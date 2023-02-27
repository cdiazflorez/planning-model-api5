package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum ProcessingType {

    ACTIVE_WORKERS,
    EFFECTIVE_WORKERS,
    ACTIVE_WORKERS_NS,
    EFFECTIVE_WORKERS_NS,
    PERFORMED_PROCESSING,
    REMAINING_PROCESSING,
    WORKERS,
    WORKERS_NS,
    MAX_CAPACITY,
    BACKLOG_LOWER_LIMIT,
    BACKLOG_UPPER_LIMIT,
    THROUGHPUT;

    private static final Map<String, ProcessingType> LOOKUP = Arrays.stream(values()).collect(
            toMap(ProcessingType::toString, Function.identity())
    );

    public static Optional<ProcessingType> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
