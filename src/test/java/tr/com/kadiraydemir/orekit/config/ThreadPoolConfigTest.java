package tr.com.kadiraydemir.orekit.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("ThreadPoolConfig Tests")
public class ThreadPoolConfigTest {

    @Test
    @DisplayName("Should create fixed thread pool when type is 'fixed'")
    public void createPropagationExecutor_fixed_returnsFixedThreadPool() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "fixed";
        config.maxThreads = 8;

        // When
        ExecutorService executor = config.createPropagationExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolExecutor);
        
        // Cleanup
        config.close(executor);
    }

    @Test
    @DisplayName("Should create fork-join pool when type is 'forkjoin'")
    public void createPropagationExecutor_forkjoin_returnsForkJoinPool() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "forkjoin";
        config.maxThreads = 8;

        // When
        ExecutorService executor = config.createPropagationExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ForkJoinPool);
        
        // Cleanup
        config.close(executor);
    }

    @Test
    @DisplayName("Should create cached thread pool when type is 'cached'")
    public void createPropagationExecutor_cached_returnsCachedThreadPool() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "cached";
        config.maxThreads = 8;

        // When
        ExecutorService executor = config.createPropagationExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolExecutor);
        
        // Cleanup
        config.close(executor);
    }

    @Test
    @DisplayName("Should default to cached thread pool for unknown type")
    public void createPropagationExecutor_default_returnsCachedThreadPool() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "unknown";
        config.maxThreads = 8;

        // When
        ExecutorService executor = config.createPropagationExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolExecutor);
        
        // Cleanup
        config.close(executor);
    }

    @Test
    @DisplayName("Should shutdown executor without error")
    public void close_shutsDownExecutor() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "fixed";
        config.maxThreads = 4;
        ExecutorService executor = config.createPropagationExecutor();

        // When - Should not throw
        assertDoesNotThrow(() -> config.close(executor));

        // Then
        assertTrue(executor.isShutdown());
    }

    @Test
    @DisplayName("Should handle case-insensitive executor type")
    public void createPropagationExecutor_caseInsensitive() {
        // Given
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.executorType = "FIXED";
        config.maxThreads = 4;

        // When
        ExecutorService executor = config.createPropagationExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolExecutor);
        
        // Cleanup
        config.close(executor);
    }
}
