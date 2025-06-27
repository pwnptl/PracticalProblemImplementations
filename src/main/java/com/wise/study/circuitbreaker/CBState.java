package com.wise.study.circuitbreaker;

public enum CBState {
    OPEN,
    HALF_OPEN,
    CLOSED; // success scenarios.
}
