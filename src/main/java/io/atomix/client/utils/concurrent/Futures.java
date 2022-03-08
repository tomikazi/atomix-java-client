// Copyright 2022-present Open Networking Foundation
// SPDX-License-Identifier: Apache-2.0

package io.atomix.client.utils.concurrent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for creating completed and exceptional futures.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public final class Futures {

    /**
     * Returns the future value when complete or if future
     * completes exceptionally returns the defaultValue.
     *
     * @param future future
     * @param defaultValue default value
     * @param <T> future value type
     * @return future value when complete or if future
     * completes exceptionally returns the defaultValue.
     */
    public static <T> T futureGetOrElse(Future<T> future, T defaultValue) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return defaultValue;
        } catch (ExecutionException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a future result with a default timeout.
     *
     * @param future the future to block
     * @param <T>    the future result type
     * @return the future result
     * @throws RuntimeException if a future exception occurs
     */
    public static <T> T get(Future<T> future) {
        return get(future, 30, TimeUnit.SECONDS);
    }

    /**
     * Gets a future result with a default timeout.
     *
     * @param future   the future to block
     * @param timeout  the future timeout
     * @param timeUnit the future timeout time unit
     * @param <T>      the future result type
     * @return the future result
     * @throws RuntimeException if a future exception occurs
     */
    public static <T> T get(Future<T> future, long timeout, TimeUnit timeUnit) {
        try {
            return future.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a future that is synchronously completed.
     *
     * @param result The future result.
     * @return The completed future.
     */
    public static <T> CompletableFuture<T> completedFuture(T result) {
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Creates a future that is asynchronously completed.
     *
     * @param result   The future result.
     * @param executor The executor on which to complete the future.
     * @return The completed future.
     */
    public static <T> CompletableFuture<T> completedFutureAsync(T result, Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> future.complete(result));
        return future;
    }

    /**
     * Creates a future that is synchronously completed exceptionally.
     *
     * @param t The future exception.
     * @return The exceptionally completed future.
     */
    public static <T> CompletableFuture<T> exceptionalFuture(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }

    /**
     * Creates a future that is asynchronously completed exceptionally.
     *
     * @param t        The future exception.
     * @param executor The executor on which to complete the future.
     * @return The exceptionally completed future.
     */
    public static <T> CompletableFuture<T> exceptionalFutureAsync(Throwable t, Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            future.completeExceptionally(t);
        });
        return future;
    }

    /**
     * Returns a future that completes callbacks in add order.
     *
     * @param <T> future value type
     * @return a new completable future that will complete added callbacks in the order in which they were added
     */
    public static <T> CompletableFuture<T> orderedFuture() {
        return new OrderedFuture<>();
    }

    /**
     * Returns a future that completes callbacks in add order.
     *
     * @param <T> future value type
     * @return a new completable future that will complete added callbacks in the order in which they were added
     */
    public static <T> CompletableFuture<T> orderedFuture(CompletableFuture<T> future) {
        CompletableFuture<T> newFuture = new OrderedFuture<>();
        future.whenComplete((r, e) -> {
            if (e == null) {
                newFuture.complete(r);
            } else {
                newFuture.completeExceptionally(e);
            }
        });
        return newFuture;
    }

    /**
     * Returns a wrapped future that will be completed on the given executor.
     *
     * @param future   the future to be completed on the given executor
     * @param executor the executor with which to complete the future
     * @param <T>      the future value type
     * @return a wrapped future to be completed on the given executor
     */
    public static <T> CompletableFuture<T> asyncFuture(CompletableFuture<T> future, Executor executor) {
        CompletableFuture<T> newFuture = new AtomixFuture<>();
        future.whenComplete((result, error) -> {
            executor.execute(() -> {
                if (error == null) {
                    newFuture.complete(result);
                } else {
                    newFuture.completeExceptionally(error);
                }
            });
        });
        return newFuture;
    }

    /**
     * Transforms exceptions on the given future using the given function.
     *
     * @param future   the future to transform
     * @param function the function with which to transform exceptions
     * @param <T>      the future type
     * @return the transformed future
     */
    public static <T> CompletableFuture<T> transformExceptions(
        CompletableFuture<T> future,
        Function<Throwable, Throwable> function) {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            if (error == null) {
                newFuture.complete(result);
            } else {
                newFuture.completeExceptionally(function.apply(error));
            }
        });
        return newFuture;
    }

    /**
     * Returns a new CompletableFuture completed with a list of computed values
     * when all of the given CompletableFuture complete.
     *
     * @param futures the CompletableFutures
     * @param <T>     value type of CompletableFuture
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<Stream<T>> allOf(Stream<CompletableFuture<T>> futures) {
        CompletableFuture<T>[] futuresArray = futures.toArray(CompletableFuture[]::new);
        return AtomixFuture.wrap(CompletableFuture.allOf(futuresArray)
            .thenApply(v -> Stream.of(futuresArray).map(CompletableFuture::join)));
    }

    /**
     * Returns a new CompletableFuture completed with a list of computed values
     * when all of the given CompletableFuture complete.
     *
     * @param futures the CompletableFutures
     * @param <T>     value type of CompletableFuture
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     */
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        return AtomixFuture.wrap(CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())));
    }

    /**
     * Returns a new CompletableFuture completed by reducing a list of computed values
     * when all of the given CompletableFuture complete.
     *
     * @param futures    the CompletableFutures
     * @param reducer    reducer for computing the result
     * @param emptyValue zero value to be returned if the input future list is empty
     * @param <T>        value type of CompletableFuture
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     */
    public static <T> CompletableFuture<T> allOf(
        List<CompletableFuture<T>> futures, BinaryOperator<T> reducer, T emptyValue) {
        return allOf(futures).thenApply(resultList -> resultList.stream().reduce(reducer).orElse(emptyValue));
    }

    public static CompletableFuture<Void> retry(Supplier<CompletableFuture<Boolean>> supplier, ThreadContext context) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        retry(supplier, context, future);
        return future;
    }

    private static void retry(
        Supplier<CompletableFuture<Boolean>> supplier,
        ThreadContext context,
        CompletableFuture<Void> future) {
        supplier.get().whenComplete((succeeded, error) -> {
            if (error == null) {
                if (succeeded) {
                    future.complete(null);
                } else {
                    context.schedule(Duration.ofMillis(10), () -> retry(supplier, context, future));
                }
            } else {
                future.completeExceptionally(error);
            }
        });
    }

    @FunctionalInterface
    public interface CheckedFunction<T, U> {
        U apply(T t) throws Exception;
    }

    /**
     * Converts the given checked function to an unchecked function.
     *
     * @param function the function to uncheck
     * @param <T>      the function argument
     * @param <U>      the function return type
     * @return an unchecked function
     */
    public static <T, U> Function<T, U> uncheck(CheckedFunction<T, U> function) {
        return uncheck(function, RuntimeException::new);
    }

    /**
     * Converts the given checked function to an unchecked function.
     *
     * @param function         the function to uncheck
     * @param exceptionFactory the exception factory to use to construct an unchecked exception
     * @param <T>              the function argument
     * @param <U>              the function return type
     * @return an unchecked function
     */
    public static <T, U> Function<T, U> uncheck(CheckedFunction<T, U> function, Function<Exception, RuntimeException> exceptionFactory) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
