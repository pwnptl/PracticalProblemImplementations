package com.wise.study.basiccircuitbreaker;

public enum CBState {
    OPEN,
    HALF_OPEN,
    CLOSED; // success scenarios.
}
