package com.jobseekercopilot.googlemapsgateway.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class GoogleMapsConfiguration {
    @Bean
    RestClient placesRestClient(GoogleMapsProperties properties) {
        return client(properties.placesBaseUrl(), properties);
    }

    @Bean
    RestClient routesRestClient(GoogleMapsProperties properties) {
        return client(properties.routesBaseUrl(), properties);
    }

    private RestClient client(String baseUrl, GoogleMapsProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
