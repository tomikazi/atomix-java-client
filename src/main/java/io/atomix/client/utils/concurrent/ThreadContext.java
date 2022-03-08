// Copyright 2022-present Open Networking Foundation
// SPDX-License-Identifier: Apache-2.0

package io.atomix.client.utils.concurrent;

import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkState;

/**
 * Thread context.
 * <p>
 * The thread context is used by Catalyst to determine the correct thread on which to execute asynchronous callbacks.
 * All threads created within Catalyst must be instances of {@link AtomixThread}. Once
 * a thread has been created, the context is stored in the thread object via
 * {@link AtomixThread#setContext(ThreadContext)}. This means there is a one-to-one relationship
 * between a context and a thread. That is, a context is representative of a thread and provides an interface for firing
 * events on that thread.
 * <p>
 * Components of the framework that provide custom threads should use {@link AtomixThreadFactory}
 * to allocate new threads and provide a custom {@link ThreadContext} implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface ThreadContext extends AutoCloseable, Executor, Scheduler {

    /**
     * Returns the current thread context.
     *
     * @return The current thread context or {@code null} if no context exists.
     */
    static ThreadContext currentContext() {
        Thread thread = Thread.currentThread();
        return thread instanceof AtomixThread ? ((AtomixThread) thread).getContext() : null;
    }

    /**
     * @throws IllegalStateException if the current thread is not a catalyst thread
     */
    static ThreadContext currentContextOrThrow() {
        ThreadContext context = currentContext();
        checkState(context != null, "not on a Catalyst thread");
        return context;
    }

    /**
     * Returns a boolean indicating whether the current thread is in this context.
     *
     * @return Indicates whether the current thread is in this context.
     */
    default boolean isCurrentContext() {
        return currentContext() == this;
    }

    /**
     * Checks that the current thread is the correct context thread.
     */
    default void checkThread() {
        checkState(currentContext() == this, "not on a Catalyst thread");
    }

    /**
     * Returns whether the thread context is currently marked blocked.
     *
     * @return whether the thread context is currently marked blocked
     */
    boolean isBlocked();

    /**
     * Marks the thread context as blocked.
     */
    void block();

    /**
     * Marks the thread context as unblocked.
     */
    void unblock();

    /**
     * Closes the context.
     */
    @Override
    void close();

}
