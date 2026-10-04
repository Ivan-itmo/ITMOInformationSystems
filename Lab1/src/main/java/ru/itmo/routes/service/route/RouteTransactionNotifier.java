package ru.itmo.routes.service.route;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import ru.itmo.routes.service.RouteChangeNotifier;

@ApplicationScoped
public class RouteTransactionNotifier {
    @Inject
    private RouteChangeNotifier changeNotifier;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void routesChangedAfterCommit() {
        notifyAfterCommit(changeNotifier::routesChanged);
    }

    public void locationsChangedAfterCommit() {
        notifyAfterCommit(() -> {
            changeNotifier.locationsChanged();
            changeNotifier.routesChanged();
        });
    }

    private void notifyAfterCommit(Runnable action) {
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    action.run();
                }
            }
        });
    }
}
