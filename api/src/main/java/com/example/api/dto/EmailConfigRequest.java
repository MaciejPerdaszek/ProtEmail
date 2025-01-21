package com.example.api.dto;

import java.util.Map;

public record EmailConfigRequest(String host, String port, String protocol, String username, String userId) {

    private static final Map<String, String> HOST_MAPPING = Map.of(
            "wp", "imap.wp.pl",
            "gmail", "imap.gmail.com",
            "onet", "imap.poczta.onet.pl"
    );

    private static final String DEFAULT_PORT = "993";
    private static final String DEFAULT_PROTOCOL = "imap";

    public EmailConfigRequest(String host, String port, String protocol, String username,
                              String userId) {
        this.host = resolveHost(host);
        this.port = port == null ? DEFAULT_PORT : port;
        this.protocol = protocol == null ? DEFAULT_PROTOCOL : protocol;
        this.username = username;
        this.userId = userId;
    }

    private static String resolveHost(String host) {
        return HOST_MAPPING.getOrDefault(host, host);
    }
}
