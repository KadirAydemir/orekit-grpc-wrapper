package tr.com.kadiraydemir.orekit.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@ApplicationScoped
public class ThreadPoolConfig {

    @ConfigProperty(name = "orekit.executor.type", defaultValue = "forkjoin")
    String executorType;

    @ConfigProperty(name = "orekit.executor.max-threads", defaultValue = "16")
    int maxThreads;

    @Produces
    @ApplicationScoped
    @Named("propagationExecutor")
    public ExecutorService createPropagationExecutor() {
        switch (executorType.toLowerCase()) {
            case "fixed":
                return Executors.newFixedThreadPool(maxThreads);
            case "forkjoin":
                return new ForkJoinPool(maxThreads);
            case "cached":
            default:
                return Executors.newCachedThreadPool();
        }
    }

    public void close(@Disposes @Named("propagationExecutor") ExecutorService executor) {
        executor.shutdown();
    }
}
