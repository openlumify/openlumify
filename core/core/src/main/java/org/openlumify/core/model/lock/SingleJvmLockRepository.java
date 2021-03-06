package org.openlumify.core.model.lock;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.ShutdownListener;
import org.openlumify.core.util.ShutdownService;

import java.util.WeakHashMap;
import java.util.concurrent.Callable;

@Singleton
public class SingleJvmLockRepository extends LockRepository implements ShutdownListener {
    private WeakHashMap<Long, Thread> threads = new WeakHashMap<>();

    // available for testing when you don't need perfect shutdown behavior
    @VisibleForTesting
    public SingleJvmLockRepository() {

    }

    @Inject
    public SingleJvmLockRepository(ShutdownService shutdownService) {
        shutdownService.register(this);
    }

    @Override
    public Lock createLock(String lockName) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    synchronized (synchronizationObject) {
                        return callable.call();
                    }
                } catch (Exception ex) {
                    throw new OpenLumifyException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        Thread t = new Thread(() -> {
            synchronized (synchronizationObject) {
                try {
                    listener.isLeader();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.setName(SingleJvmLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
        t.setDaemon(true);
        t.start();
        threads.put(t.getId(), t);
    }

    @Override
    public void shutdown() {
        for (Thread thread : threads.values()) {
            thread.interrupt();
        }
    }
}
