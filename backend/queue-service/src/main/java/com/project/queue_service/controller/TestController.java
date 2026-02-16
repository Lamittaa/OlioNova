package com.project.queue_service.controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class TestController {

    @MessageMapping("/chat")
    @SendTo("/topic/queue")
    private String handle(String message) {
        return "Server received:" + message;
    }

}