/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.application.util;

import org.wso2.carbon.identity.framework.async.status.mgt.models.dos.UnitOperationRecord;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This data structure holds status of unit share operations, in order to determine
 * the overall status of the asynchronous operation.
 */
public class AsyncOperationStatusHolderQueue implements Iterable<UnitOperationRecord> {

    private final ConcurrentLinkedQueue<UnitOperationRecord> queue;

    public ConcurrentLinkedQueue<UnitOperationRecord> getQueue() {

        return queue;
    }

    public AsyncOperationStatusHolderQueue() {

        this.queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Add a new operation context to the queue.
     *
     * @param context The async operation context to add.
     */
    public void addOperationStatus(UnitOperationRecord context) {

        queue.add(context);
    }

    /**
     * Retrieve and remove the next operation context from the queue.
     *
     * @return The next async operation context or null if the queue is empty.
     */
    public UnitOperationRecord pollOperationContext() {

        return queue.poll();
    }

    /**
     * Peek at the next operation context without removing it.
     *
     * @return The next async operation context or null if the queue is empty.
     */
    public UnitOperationRecord peekOperationContext() {

        return queue.peek();
    }

    /**
     * Returns an iterator over the operation contexts in the queue.
     *
     * @return Iterator for the queue.
     */
    public Iterator<UnitOperationRecord> iterator() {

        return queue.iterator();
    }

    /**
     * Check if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise.
     */
    public boolean isEmpty() {

        return queue.isEmpty();
    }

    /**
     * Get the number of operation contexts in the queue.
     *
     * @return The size of the queue.
     */
    public int getQueueSize() {

        return queue.size();
    }

    /**
     * Clears all operation contexts from the queue.
     */
    public void clearQueue() {

        queue.clear();
    }
}
