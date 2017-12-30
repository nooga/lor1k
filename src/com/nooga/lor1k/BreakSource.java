package com.nooga.lor1k;

public interface BreakSource {
    boolean shouldStop(int addr);
    void executionStopped(int addr);
}
