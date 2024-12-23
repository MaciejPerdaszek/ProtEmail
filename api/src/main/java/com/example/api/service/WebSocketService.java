package com.example.api.service;

import java.io.IOException;
import org.springframework.web.socket.WebSocketSession;

public interface WebSocketService {

    void connect(String email, String session);

    void disconnect(String email);

    void sendMessage(String email, String message);
}
