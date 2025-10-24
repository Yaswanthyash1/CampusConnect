package com.collegeclubs.event_service.service;

import com.collegeclubs.event_service.model.Event;
import com.collegeclubs.event_service.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    @Autowired
    private EventRepository eventRepository;

    public List<Event> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            setDescriptionPreview(event);
        }
        return events;
    }

    public Event getEventById(Long id) {
        Optional<Event> event = eventRepository.findById(id);
        if (event.isPresent()) {
            setDescriptionPreview(event.get());
            return event.get();
        }
        return null;
    }

    public List<Event> getUpcomingEvents() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return eventRepository.findByTimestampAfterOrderByTimestampAsc(now);
    }

    public List<Event> getPastEvents() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return eventRepository.findByTimestampBeforeOrderByTimestampDesc(now);
    }

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    private void setDescriptionPreview(Event event) {
        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            event.setDescriptionPreview("No description available.");
        } else if (event.getDescription().length() > 100) {
            event.setDescriptionPreview(event.getDescription().substring(0, 100) + "...");
        } else {
            event.setDescriptionPreview(event.getDescription());
        }
    }
}