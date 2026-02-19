package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.TunnelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KubernetesTunnelService {

    private static final int MIN_PORT = 9000;
    private static final int MAX_PORT = 11000;
    private static final int TUNNEL_STARTUP_TIMEOUT_SECONDS = 30;

    private final Random random = new Random();

    public boolean validateKubectl() {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "version", "--client");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("kubectl version command timed out");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("kubectl is not installed or not accessible");
                return false;
            }
            
            log.debug("kubectl is installed and accessible");
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to validate kubectl", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String discoverPod(String namespace, String podPrefix) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "kubectl", "get", "pods",
            "-n", namespace,
            "--no-headers",
            "-o", "custom-columns=:metadata.name"
        );
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("kubectl get pods command timed out");
        }
        
        if (process.exitValue() != 0) {
            throw new IOException("kubectl get pods failed with exit code: " + process.exitValue());
        }
        
        String[] pods = output.toString().split("\n");
        for (String pod : pods) {
            pod = pod.trim();
            if (!pod.isEmpty() && pod.startsWith(podPrefix)) {
                log.info("Discovered pod: {} in namespace: {} with prefix: {}", pod, namespace, podPrefix);
                return pod;
            }
        }
        
        throw new IOException("No pod found matching prefix: " + podPrefix + " in namespace: " + namespace);
    }

    public int findAvailablePort() throws IOException {
        int attempts = 0;
        while (attempts < 100) {
            int port = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT + 1);
            if (isPortAvailable(port)) {
                log.debug("Found available port: {}", port);
                return port;
            }
            attempts++;
        }
        throw new IOException("Failed to find available port in range " + MIN_PORT + "-" + MAX_PORT);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Process startTunnel(TunnelConfig config, int hostPort) throws IOException, InterruptedException {
        String podName = discoverPod(config.getNamespace(), config.getPodPrefix());
        
        log.info("Starting kubectl port-forward for pod: {} in namespace: {} on local port: {} to pod port: {}",
                podName, config.getNamespace(), hostPort, config.getPodPort());
        
        ProcessBuilder pb = new ProcessBuilder(
            "kubectl", "port-forward",
            "pod/" + podName,
            hostPort + ":" + config.getPodPort(),
            "-n", config.getNamespace()
        );
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        if (!waitForTunnelReady(process, hostPort, TUNNEL_STARTUP_TIMEOUT_SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Tunnel failed to start within timeout");
        }
        
        log.info("Tunnel started successfully for pod: {} on local port: {}", podName, hostPort);
        return process;
    }

    private boolean waitForTunnelReady(Process process, int port, int timeoutSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000) {
            if (!process.isAlive()) {
                log.error("Tunnel process died during startup");
                return false;
            }
            
            if (!isPortAvailable(port)) {
                log.debug("Port {} is now bound, tunnel is ready", port);
                return true;
            }
            
            Thread.sleep(500);
        }
        
        log.warn("Timeout waiting for tunnel to start on port {}", port);
        return false;
    }

    public void stopTunnel(Process process) {
        if (process != null && process.isAlive()) {
            log.info("Stopping kubectl tunnel process");
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
