package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.wso2.carbon.identity.framework.async.status.mgt.models.dos.UnitOperationRecord;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

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
}