/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.astyanax.connectionpool.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.astyanax.connectionpool.ConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.HostConnectionPool;
import com.netflix.astyanax.connectionpool.HostStats;
import com.netflix.astyanax.connectionpool.exceptions.PoolTimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.TimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.NoAvailableHostsException;
import com.netflix.astyanax.connectionpool.exceptions.OperationTimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.connectionpool.exceptions.HostDownException;
import com.netflix.astyanax.connectionpool.exceptions.TransportException;
import com.netflix.astyanax.connectionpool.exceptions.InterruptedOperationException;

/**
 * 
 * Impl for {@link ConnectionPoolMonitor} that employs counters to track stats such as 
 * 
 * <ol>
 * <li> operation success / failures / timeouts / socket timeouts / interrupted </li>
 * <li> connection created /  borrowed / returned / closed / create failures </li>
 * <li> hosts added /  removed / marked as down / reactivated </li>
 * <li> transport failures and other useful stats </li>
 * </ol>
 * 
 * @author elandau
 */
public class CountingConnectionPoolMonitor implements ConnectionPoolMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(CountingConnectionPoolMonitor.class);

    private final AtomicLong operationFailureCount = new AtomicLong();
    private final AtomicLong operationSuccessCount = new AtomicLong();
    private final AtomicLong connectionCreateCount = new AtomicLong();
    private final AtomicLong connectionClosedCount = new AtomicLong();
    private final AtomicLong connectionCreateFailureCount = new AtomicLong();
    private final AtomicLong connectionBorrowCount = new AtomicLong();
    private final AtomicLong connectionReturnCount = new AtomicLong();

    private final AtomicLong operationFailoverCount = new AtomicLong();

    private final AtomicLong hostAddedCount = new AtomicLong();
    private final AtomicLong hostRemovedCount = new AtomicLong();
    private final AtomicLong hostDownCount = new AtomicLong();
    private final AtomicLong hostReactivatedCount = new AtomicLong();

    private final AtomicLong poolExhastedCount = new AtomicLong();
    private final AtomicLong operationTimeoutCount = new AtomicLong();
    private final AtomicLong socketTimeoutCount = new AtomicLong();
    private final AtomicLong noHostsCount = new AtomicLong();
    private final AtomicLong unknownErrorCount = new AtomicLong();
    private final AtomicLong badRequestCount = new AtomicLong();
    private final AtomicLong interruptedCount = new AtomicLong();
    private final AtomicLong transportErrorCount = new AtomicLong();

    private final AtomicLong notFoundCounter = new AtomicLong();
    
    public CountingConnectionPoolMonitor() {
    }
    
    private void trackError(Host host, Exception reason) {
        if (reason instanceof PoolTimeoutException) {
            this.poolExhastedCount.incrementAndGet();
        }
        else if (reason instanceof TimeoutException) {
            this.socketTimeoutCount.incrementAndGet();
        }
        else if (reason instanceof OperationTimeoutException) {
            this.operationTimeoutCount.incrementAndGet();
        }
        else if (reason instanceof BadRequestException) {
            this.badRequestCount.incrementAndGet();
        }
        else if (reason instanceof NoAvailableHostsException ) {
            this.noHostsCount.incrementAndGet();
        }
        else if (reason instanceof InterruptedOperationException) {
            this.interruptedCount.incrementAndGet();
        }
        else if (reason instanceof HostDownException) {
            this.hostDownCount.incrementAndGet();
        }
        else if (reason instanceof TransportException) {
            this.transportErrorCount.incrementAndGet();
        }
        else {
            LOG.error(reason.toString(), reason);
            this.unknownErrorCount.incrementAndGet();
        }
    }

    @Override
    public void incOperationFailure(Host host, Exception reason) {
        if (reason instanceof NotFoundException) {
            this.notFoundCounter.incrementAndGet();
            return;
        }
        
        this.operationFailureCount.incrementAndGet();
        trackError(host, reason);
    }

    public long getOperationFailureCount() {
        return this.operationFailureCount.get();
    }

    @Override
    public void incOperationSuccess(Host host, long latency) {
        this.operationSuccessCount.incrementAndGet();
    }

    public long getOperationSuccessCount() {
        return this.operationSuccessCount.get();
    }

    @Override
    public void incConnectionCreated(Host host) {
        this.connectionCreateCount.incrementAndGet();
    }

    public long getConnectionCreatedCount() {
        return this.connectionCreateCount.get();
    }

    @Override
    public void incConnectionClosed(Host host, Exception reason) {
        this.connectionClosedCount.incrementAndGet();
    }

    public long getConnectionClosedCount() {
        return this.connectionClosedCount.get();
    }

    @Override
    public void incConnectionCreateFailed(Host host, Exception reason) {
        this.connectionCreateFailureCount.incrementAndGet();
    }

    public long getConnectionCreateFailedCount() {
        return this.connectionCreateFailureCount.get();
    }

    @Override
    public void incConnectionBorrowed(Host host, long delay) {
        this.connectionBorrowCount.incrementAndGet();
    }

    public long getConnectionBorrowedCount() {
        return this.connectionBorrowCount.get();
    }

    @Override
    public void incConnectionReturned(Host host) {
        this.connectionReturnCount.incrementAndGet();
    }

    public long getConnectionReturnedCount() {
        return this.connectionReturnCount.get();
    }

    public long getPoolExhaustedTimeoutCount() {
        return this.poolExhastedCount.get();
    }

    @Override
    public long getSocketTimeoutCount() {
        return this.socketTimeoutCount.get();
    }
    
    public long getOperationTimeoutCount() {
        return this.operationTimeoutCount.get();
    }

    @Override
    public void incFailover(Host host, Exception reason) {
        this.operationFailoverCount.incrementAndGet();
        trackError(host, reason);
    }

    @Override
    public long getFailoverCount() {
        return this.operationFailoverCount.get();
    }

    @Override
    public void onHostAdded(Host host, HostConnectionPool<?> pool) {
        LOG.info("AddHost: " + host.getHostName());
        this.hostAddedCount.incrementAndGet();
    }

    @Override
    public long getHostAddedCount() {
        return this.hostAddedCount.get();
    }

    @Override
    public void onHostRemoved(Host host) {
        LOG.info("RemoveHost: " + host.getHostName());
        this.hostRemovedCount.incrementAndGet();
    }

    @Override
    public long getHostRemovedCount() {
        return this.hostRemovedCount.get();
    }

    @Override
    public void onHostDown(Host host, Exception reason) {
        this.hostDownCount.incrementAndGet();
    }

    @Override
    public long getHostDownCount() {
        return this.hostDownCount.get();
    }

    @Override
    public void onHostReactivated(Host host, HostConnectionPool<?> pool) {
        LOG.info("Reactivating " + host.getHostName());
        this.hostReactivatedCount.incrementAndGet();
    }

    public long getHostReactivatedCount() {
        return this.hostReactivatedCount.get();
    }

    @Override
    public long getNoHostCount() {
        return this.noHostsCount.get();
    }

    @Override
    public long getUnknownErrorCount() {
        return this.unknownErrorCount.get();
    }
    
    @Override
    public long getInterruptedCount() {
        return this.interruptedCount.get();
    }

    @Override
    public long getTransportErrorCount() {
        return this.transportErrorCount.get();
    }

    @Override
    public long getBadRequestCount() {
        return this.badRequestCount.get();
    }

    public long getNumBusyConnections() {
        return this.connectionBorrowCount.get() - this.connectionReturnCount.get();
    }

    public long getNumOpenConnections() {
        return this.connectionCreateCount.get() - this.connectionClosedCount.get();
    }
    
    @Override
    public long notFoundCount() {
        return this.notFoundCounter.get();
    }

    @Override
    public long getHostCount() {
        return getHostAddedCount() - getHostRemovedCount();
    }

    @Override
    public long getHostActiveCount() {
        return hostAddedCount.get() - hostRemovedCount.get() + hostReactivatedCount.get() - hostDownCount.get();
    }

    public String toString() {
        // Build the complete status string
        return "CountingConnectionPoolMonitor(" + "Connections[" +  "open=" + getNumOpenConnections() + ",busy=" + getNumBusyConnections() + ",create=" + connectionCreateCount.get() + ",close=" + connectionClosedCount.get() + ",failed=" + connectionCreateFailureCount.get() + ",borrow=" + connectionBorrowCount.get() + ",return=" + connectionReturnCount.get() + "], Operations[" +  "success=" + operationSuccessCount.get() + ",failure=" + operationFailureCount.get() + ",optimeout=" + operationTimeoutCount.get() + ",timeout=" + socketTimeoutCount.get() + ",failover=" + operationFailoverCount.get() + ",nohosts=" + noHostsCount.get() + ",unknown=" + unknownErrorCount.get() + ",interrupted=" + interruptedCount.get() + ",exhausted=" + poolExhastedCount.get() + ",transport=" + transportErrorCount.get() + "], Hosts[" +  "add=" + hostAddedCount.get() + ",remove=" + hostRemovedCount.get() + ",down=" + hostDownCount.get() + ",reactivate=" + hostReactivatedCount.get() + ",active=" + getHostActiveCount() + "])";
    }

    @Override
    public Map<Host, HostStats> getHostStats() {
        throw new UnsupportedOperationException("Not supported");
    }
}
