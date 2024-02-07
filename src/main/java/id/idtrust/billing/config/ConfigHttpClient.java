package id.idtrust.billing.config;

import brave.http.HttpTracing;
import brave.httpclient.TracingHttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@EnableAutoConfiguration
@Configuration
public class ConfigHttpClient {

    /**
     * Apache HC clients aren't traced by default. This creates a traced instance.
     */
    /** Apache HC clients aren't traced by default. This creates a traced instance. */
    @Bean
    HttpClient httpClient(HttpTracing httpTracing) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);


        return TracingHttpClientBuilder.create(httpTracing).setSSLSocketFactory(scsf).build();
    }
}
