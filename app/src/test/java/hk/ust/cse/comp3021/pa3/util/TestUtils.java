package hk.ust.cse.comp3021.pa3.util;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.opentest4j.TestAbortedException;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    public static int repeatTest(ThrowingSupplier<Boolean> runner, int repeat, int timeout) throws TestAbortedException {
        return repeatTest(runner, repeat, timeout, false);
    }

    public static int repeatTest(ThrowingSupplier<Boolean> runner, int repeat, int timeout, boolean parallel) throws TestAbortedException {
        return repeatTest(runner, repeat, timeout, parallel, it -> false);
    }

    public static int repeatTest(ThrowingSupplier<Boolean> runner, int repeat, int timeout, boolean parallel,
                                 Function<Boolean, Boolean> abort) throws TestAbortedException {
        var pool = Executors.newFixedThreadPool(parallel ? repeat : 1);
        var abortException = new AtomicReference<TestAbortedException>(null);
        var trueCount = (int) Arrays.stream(new Boolean[repeat])
                .map(it -> {
                    try {
                        return pool.submit(() -> {
                            try {
                                return runner.get();
                            } catch (Throwable e) {
                                if (e instanceof TestAbortedException abortedException) {
                                    abortException.set(abortedException);
                                }
                                return false;
                            }
                        });
                    } catch (Throwable e) {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .map(fu -> {
                    try {
                        var runResult = fu.get(timeout, TimeUnit.MILLISECONDS);
                        var whetherAbort = abort.apply(runResult);
                        if (whetherAbort) {
                            System.out.println("repeatTest abort");
                            pool.shutdownNow();
                        }
                        return runResult;
                    } catch (InterruptedException | TimeoutException | ExecutionException ignored) {
                        return false;
                    }
                })
                .filter(r -> r)
                .count();
        if (abortException.get() != null) {
            throw abortException.get();
        }
        return trueCount;
    }

    public static void assertMostly(ThrowingSupplier<Boolean> runner, int repeat, int timeout) {
        assertMostly(runner, repeat, timeout, false);
    }

    public static void assertMostly(ThrowingSupplier<Boolean> runner, int repeat, int timeout, boolean parallel) {
        var totalCount = new AtomicInteger(0);
        var successCount = new AtomicInteger(0);
        var c = repeatTest(runner, repeat, timeout, parallel, success -> {
            totalCount.incrementAndGet();
            if (success && successCount.incrementAndGet() > repeat / 2) {
                return true;
            }
            return repeat - totalCount.get() <= repeat / 2 - successCount.get();
        });
        assertThat(c, Matchers.greaterThan(repeat / 2));
    }

    public static void assertAlways(ThrowingRunnable runner, int repeat, int timeout) {
        assertAlways(runner, repeat, timeout, false);
    }

    public static void assertAlways(ThrowingRunnable runner, int repeat, int timeout, boolean parallel) {
        var c = repeatTest(() -> {
            try {
                runner.run();
                return true;
            } catch (Throwable e) {
                if (e instanceof TestAbortedException) {
                    throw e;
                }
                return false;
            }
        }, repeat, timeout, parallel, success -> !success);
        assertEquals(repeat, c);
    }

    public static <T> T assumeDoesNotThrow(ThrowingSupplier<T> runner) {
        try {
            return runner.get();
        } catch (Throwable throwable) {
            throw new TestAbortedException(throwable.toString());
        }
    }
}
