package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.TunnelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KubernetesTunnelService {

    private static final int MIN_PORT = 9000;
    private static final int MAX_PORT = 11000;

    public boolean validateKubectl() {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "version", "--client");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume stdout/stderr to prevent deadlock when OS pipe buffer fills up.
            // Without this, process.waitFor() can block forever if output exceeds ~64KB.
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("kubectl version command timed out");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("kubectl is not installed or not accessible (exit code {}): {}", exitCode, output);
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
            int port = MIN_PORT + ThreadLocalRandom.current().nextInt(MAX_PORT - MIN_PORT + 1);
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
            return socket != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Tests whether a TCP connection can be established on the given port.
     * Used to verify that kubectl port-forward has bound and is ready to forward traffic.
     */
    private boolean canConnect(int port) {
        try (var socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
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
            "--address", "127.0.0.1",
            "pod/" + podName,
            hostPort + ":" + config.getPodPort(),
            "-n", config.getNamespace()
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));

        Process process = pb.start();

        // Poll for tunnel readiness: try connecting to the port until it responds or timeout.
        long deadline = System.currentTimeMillis() + config.getTunnelReadyTimeoutMs();
        long pollIntervalMs = 200;
        log.info("Waiting for tunnel to become ready on port {} (timeout: {}ms)...", hostPort, config.getTunnelReadyTimeoutMs());

        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new IOException("Tunnel process died during startup");
            }
            if (canConnect(hostPort)) {
                log.info("Tunnel ready on port {} after {}ms", hostPort,
                    config.getTunnelReadyTimeoutMs() - (deadline - System.currentTimeMillis()));
                return process;
            }
            Thread.sleep(pollIntervalMs);
        }

        // Timeout — clean up
        process.destroyForcibly();
        throw new IOException("Tunnel failed to become ready on port " + hostPort +
            " within " + config.getTunnelReadyTimeoutMs() + "ms");
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
