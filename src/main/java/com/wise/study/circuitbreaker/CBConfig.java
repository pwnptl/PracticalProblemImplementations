package com.wise.study.circuitbreaker;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

@Builder
@Getter
public final class CBConfig {
    private double failureRatioToOpen = 0.1; // more than 30% request Failed -> should open the circuit.
    private double successRatioToClose=0.7;
    private Duration openDuration = Duration.ofSeconds(5);
    private Duration closedObservationDuration;
    private Duration halfOpenDuration = Duration.ofSeconds(2);
    private int requestAllowedInHalfOpen = 20;
    private Duration revaluateStateDuration = Duration.ofSeconds(1);

    public static CBConfig defaultConfig() {
        return CBConfig.builder()
                .failureRatioToOpen(0.05)
                .successRatioToClose(0.08)
                .openDuration(Duration.ofSeconds(3))
                .closedObservationDuration(Duration.ofSeconds(10))
                .halfOpenDuration(Duration.ofSeconds(5))
                .requestAllowedInHalfOpen(8)
                .revaluateStateDuration(Duration.ofSeconds(5))
                .build();
    }
}
