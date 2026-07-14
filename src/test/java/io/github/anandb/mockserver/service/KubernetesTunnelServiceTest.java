package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.TunnelConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KubernetesTunnelService Tests")
class KubernetesTunnelServiceTest {

    private final KubernetesTunnelService service = new KubernetesTunnelService();

    @Mock
    private Process mockProcess;

    @Test
    void findAvailablePortReturnsPortInValidRange() throws IOException {
        int port = service.findAvailablePort();
        assertTrue(port >= 9000 && port <= 11000,
            "Port " + port + " should be between 9000 and 11000");
    }

    @Test
    void validateKubectlDoesNotThrow() {
        // Verify the method doesn't throw regardless of environment
        assertDoesNotThrow(() -> service.validateKubectl());
    }

    @Test
    void stopTunnelWithNullDoesNotThrow() {
        assertDoesNotThrow(() -> service.stopTunnel(null));
    }

    @Test
    void stopTunnelWithDeadProcessDoesNotThrow() {
        when(mockProcess.isAlive()).thenReturn(false);
        assertDoesNotThrow(() -> service.stopTunnel(mockProcess));
    }

    @Test
    void stopTunnelKillsAliveProcess() throws InterruptedException {
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);

        service.stopTunnel(mockProcess);

        verify(mockProcess).destroyForcibly();
    }

    @Test
    void stopTunnelHandlesInterruptedException() throws InterruptedException {
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.waitFor(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        service.stopTunnel(mockProcess);

        verify(mockProcess, times(2)).destroyForcibly();
        assertTrue(Thread.currentThread().isInterrupted());
        // Clean up interrupt flag
        Thread.interrupted();
    }
}
