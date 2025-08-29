package com.service;

import com.model.Event;
import java.util.List;

public class EventServiceProxy {
    private final EventService eventService;

    public EventServiceProxy(EventService eventService) {
        this.eventService = eventService;
    }

    public List<Event> getAllEvents() {
        // You can add proxy logic here if needed
        return eventService.getAllEvents();
    }
}

