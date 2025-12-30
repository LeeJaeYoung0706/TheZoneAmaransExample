package com.lnk.prinics.system.thezoneApi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amarans-api")
public record ThezoneApiProperties(
        String baseUrl,
        String hashKey,
        String token,
        String calleName,
        String groupSeq
) {}
