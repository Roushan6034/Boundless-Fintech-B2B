package com.boundless.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.urlconnection.URLConnectionSender;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.brave.MutableSpanBytesEncoder;
import brave.handler.MutableSpan;

@Configuration
public class TracingConfig {

    @Value("${management.tracing.export.zipkin.endpoint:http://localhost:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Bean
    public BytesMessageSender zipkinSender() {
        return URLConnectionSender.create(zipkinEndpoint);
    }

    @Bean
    public BytesEncoder<MutableSpan> bytesEncoder() {
        return MutableSpanBytesEncoder.JSON_V2;
    }
}
