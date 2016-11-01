package com.rsw.stream.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Created by dalms on 10/26/16.
 */
@Configuration
@EnableAsync
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(HttpComponentsClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(clientHttpRequestFactory);

        return template;
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        factory.setReadTimeout(10000);
        factory.setConnectTimeout(10000);
        // setting this to false is how we ensure the RestTemplate carries the stream through without fully uploading
        factory.setBufferRequestBody(false);

        HttpClientBuilder builder = HttpClientBuilder.create()
                .setMaxConnTotal(10)
                .setMaxConnPerRoute(10);
        HttpClient client = builder.build();
        factory.setHttpClient(client);
        return factory;
    }
}
