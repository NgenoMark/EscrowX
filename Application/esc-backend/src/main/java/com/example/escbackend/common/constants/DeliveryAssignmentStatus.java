package com.example.escbackend.common.constants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DeliveryAssignmentStatus {
    ASSIGNED,
    ACCEPTED,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED_AT_BUYER,
    DELIVERED_TO_BUYER,
    FAILED,
    CANCELLED;

    public String value() {
        return name();
    }

    public static List<String> valuesOf(DeliveryAssignmentStatus... statuses) {
        return Arrays.stream(statuses)
            .map(DeliveryAssignmentStatus::value)
            .collect(Collectors.toList());
    }
}