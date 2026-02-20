package io.github.anandb.mockserver.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching HttpClient instances.
 * Provides support for insecure clients that ignore SSL certificate errors.
 */
@Slf4j
@Component
public class HttpClientFactory {

    private final Map<Boolean, HttpClient> clientCache = new ConcurrentHashMap<>();

    /**
     * Returns an HttpClient instance.
     *
     * @param ignoreSSLErrors if true, returns a client that ignores SSL certificate errors
     * @return an HttpClient instance
     */
    public HttpClient getHttpClient(boolean ignoreSSLErrors) {
        return clientCache.computeIfAbsent(ignoreSSLErrors, this::createHttpClient);
    }

    private HttpClient createHttpClient(boolean ignoreSSLErrors) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        if (ignoreSSLErrors) {
            log.warn("Creating insecure HttpClient that ignores SSL certificate errors");
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                X509TrustManager trustAllManager = new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());

                builder.sslContext(sslContext);
                // Hostname verification is not directly exposed in the same way as SSLContext in Java 11 HttpClient,
                // but setting a permissive SSLContext handles most certificate issues.
                // For proper hostname verification bypass in Java HttpClient, one might need to set a property
                // or use a more complex workaround, but usually this is sufficient for 'ignore certificate errors'.
                System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.error("Failed to create insecure SSL context", e);
                throw new RuntimeException("Failed to create insecure HttpClient", e);
            }
        }

        return builder.build();
    }
}
