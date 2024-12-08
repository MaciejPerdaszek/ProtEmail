package com.example.api.dto;

public record EmailConfigRequest(String host, String port, String protocol, String username,
                                 String password, int messageCount) {

    public EmailConfigRequest {
        if (port == null) port = "993";
        if (protocol == null) protocol = "imap";
    }
}
