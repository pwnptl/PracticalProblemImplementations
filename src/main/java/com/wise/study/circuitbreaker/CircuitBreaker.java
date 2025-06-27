package com.wise.study.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class CircuitBreaker {

    private static final Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    private final String name;
    private int successCount;
    private int failureCount;
    private final BlockingQueue<CBElement> threadSafeQueue; // thread safe
    @Getter
    private CBState cbState;
    private long lastOpenEpochMillis;
    private final CBConfig cbConfig;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Getter
    protected class CBElement {
        private final long epochMillis;
        private boolean isSuccess;

        CBElement(boolean isSuccess) {
            this.epochMillis = Instant.now().toEpochMilli();
            this.isSuccess = isSuccess;
        }

    }

    public static CircuitBreaker get(String cbName, CBConfig cbConfig) {
        if (!circuitBreakerMap.containsKey(cbName)) {
            log.info("Created CB of name = {}", cbName);
            circuitBreakerMap.put(cbName, new CircuitBreaker(cbName, cbConfig));
        }
        return circuitBreakerMap.get(cbName);
    }


    private CircuitBreaker(String name, CBConfig cbConfig) {
        this.name = name;
        if (cbConfig == null)
            cbConfig = CBConfig.defaultConfig();

        this.cbConfig = cbConfig;
        this.lastOpenEpochMillis = Instant.now().toEpochMilli() - 1
                - cbConfig.getOpenDuration().toMillis() - cbConfig.getHalfOpenDuration().toMillis();
        this.successCount = 0;
        this.failureCount = 0;
        this.threadSafeQueue = new LinkedBlockingDeque<>();
        this.cbState = CBState.CLOSED;
        this.scheduler.scheduleAtFixedRate(this::evaluateStatus, 0, cbConfig.getRevaluateStateDuration().toMillis(), TimeUnit.MILLISECONDS);
    }


    public boolean isClosed() {
        if (CBState.HALF_OPEN.equals(cbState)) {
            removeStaleRequests();
            boolean res = threadSafeQueue.size() < cbConfig.getRequestAllowedInHalfOpen();
//            log.info("circuit {} is half open, returning {}", name, res);
            return res;
        }
        return CBState.CLOSED.equals(cbState);
    }

    public void appendRequest(boolean isSuccess) {
        removeStaleRequests();
        if (CBState.CLOSED.equals(cbState)
                || (CBState.HALF_OPEN.equals(cbState) && threadSafeQueue.size() < cbConfig.getRequestAllowedInHalfOpen())) {
            CBElement cbElement = new CBElement(isSuccess);
            threadSafeQueue.offer(cbElement);
            if (isSuccess) successCount++;
            else failureCount++;
        }

    }

    @Scheduled(fixedRate = 1000) // should be config
    private void evaluateStatus() {

        log.info("failureCount {}, successCount {}", failureCount, successCount);
        removeStaleRequests();
        if (CBState.CLOSED.equals(cbState)) {
            if (getCurrentFailureRatio() > cbConfig.getFailureRatioToOpen()) {
                openCircuit();
            }
        } else if (CBState.HALF_OPEN.equals(cbState)) {
            long currentEpoch = Instant.now().toEpochMilli();
            if (currentEpoch - cbConfig.getHalfOpenDuration().toMillis() > lastOpenEpochMillis) {
                if (getCurrentSuccessRatio() >= cbConfig.getSuccessRatioToClose()) {
                    log.info("Closing circuit : {}", name);
                    cbState = CBState.CLOSED;
                } else
                    openCircuit();
            }
        } else {
            long currentEpoch = Instant.now().toEpochMilli();
            if (currentEpoch - cbConfig.getOpenDuration().toMillis() - cbConfig.getHalfOpenDuration().toMillis()
                    > lastOpenEpochMillis) {
                cbState = CBState.HALF_OPEN;
            }
        }
        log.info("evaluated CB {} State : {}", name, cbState.name());
    }

    private void openCircuit() {
        log.info("Opening circuit : {}", name);
        if (CBState.OPEN.equals(cbState))
            return;
        cbState = CBState.OPEN;
        failureCount = 0;
        successCount = 0;
        lastOpenEpochMillis = Instant.now().toEpochMilli();
        threadSafeQueue.clear();
    }

    private void removeStaleRequests() {
        long currentEpoch = Instant.now().toEpochMilli();
        while (!threadSafeQueue.isEmpty() && threadSafeQueue.peek().getEpochMillis() < currentEpoch - cbConfig.getClosedObservationDuration().toMillis()) {
            removeRequestElement();
        }
    }

    private double getCurrentFailureRatio() {
        if (successCount + failureCount == 0)
            return 0;
        return (1.0 * failureCount) / (successCount + failureCount);
    }

    private double getCurrentSuccessRatio() {
        return 1 - getCurrentFailureRatio();
    }

    void removeRequestElement() {
        CBElement element = threadSafeQueue.poll();
        if (element != null)
            if (element.isSuccess)
                successCount--;
            else
                failureCount--;
    }
}
