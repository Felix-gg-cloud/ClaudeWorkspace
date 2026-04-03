package com.ll.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.proxy.enabled", havingValue = "true")
public class ProxyConfig {

    @Value("${app.proxy.host}")
    private String host;

    @Value("${app.proxy.port:8080}")
    private int port;

    @PostConstruct
    public void configureProxy() {
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", String.valueOf(port));
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", String.valueOf(port));
        System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|*.local");
    }
}
