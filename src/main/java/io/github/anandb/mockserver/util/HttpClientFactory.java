package io.github.anandb.mockserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
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
@Component
public class HttpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);
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
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        if (ignoreSSLErrors) {
            log.warn("Creating insecure HttpClient that ignores SSL certificate errors");
            try {
                X509TrustManager trustAllManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {trustAllManager}, new SecureRandom());
                builder.sslContext(sslContext);
                // Disable hostname verification per-client via SSLParameters instead of
                // using System.setProperty which affects the entire JVM globally.
                SSLParameters sslParams = sslContext.getDefaultSSLParameters();
                sslParams.setEndpointIdentificationAlgorithm(null);
                builder.sslParameters(sslParams);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.error("Failed to create insecure SSL context", e);
                throw new RuntimeException("Failed to create insecure HttpClient", e);
            }
        }
        return builder.build();
    }
}
