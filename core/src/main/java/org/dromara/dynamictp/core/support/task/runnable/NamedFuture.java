package org.dromara.dynamictp.core.support.task.runnable;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.support.task.runnable.NamedRunnable;

public class NamedFuture<V> extends NamedRunnable {
    private final FutureTask<V> futureTask;

    public NamedFuture(Callable<V> callable, String name) {
        super(null, name); // Name is passed to the superclass
        this.futureTask = new FutureTask<>(callable);
    }

    public NamedFuture(Runnable runnable, V result, String name) {
        super(null, name); // Name is passed to the superclass
        this.futureTask = new FutureTask<>(runnable, result);
    }

    public FutureTask getFutureTask() {
        return this.futureTask;
    }

    @Override
    public void run() {
        this.futureTask.run(); // Delegate to the FutureTask's run method
    }

    public boolean isDone() {
        return this.futureTask.isDone();
    }

    public V get() throws Exception {
        return this.futureTask.get(); // Get the result from FutureTask
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.futureTask.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return this.futureTask.isCancelled();
    }

    public static <V> NamedFuture<V> of(Callable<V> callable, String name) {
        if (StringUtils.isBlank(name)) {
            name = callable.getClass().getSimpleName() + "-" + UUID.randomUUID();
        }
        return new NamedFuture<>(callable, name);
    }

    public static <V> NamedFuture<V> of(Runnable runnable, V result, String name) {
        if (StringUtils.isBlank(name)) {
            name = runnable.getClass().getSimpleName() + "-" + UUID.randomUUID();
        }
        return new NamedFuture<>(runnable, result, name);
    }
}
