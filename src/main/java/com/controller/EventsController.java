package com.controller;

import com.model.Event;
import com.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Controller
public class EventsController {
    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/events")
    public String eventsDefault() {
        // default to upcoming
        return "redirect:/events/upcoming";
    }

    @GetMapping("/events/upcoming")
    public String upcomingEvents(Model model) {
        Timestamp now = Timestamp.from(Instant.now());
        List<Event> events = eventRepository.findByTimestampAfterOrderByTimestampAsc(now);
        model.addAttribute("events", events);
        model.addAttribute("title", "Upcoming Events");
        model.addAttribute("description", "Discover and register for exciting events happening at NITK");
        model.addAttribute("isUpcoming", true);
        return "events";
    }

    @GetMapping("/events/past")
    public String pastEvents(Model model) {
        Timestamp now = Timestamp.from(Instant.now());
        List<Event> events = eventRepository.findByTimestampBeforeOrderByTimestampDesc(now);
        model.addAttribute("events", events);
        model.addAttribute("title", "Past Events");
        model.addAttribute("description", "Browse completed events and their details.");
        model.addAttribute("isUpcoming", false);
        return "events";
    }

    // legacy/alternate paths
    @GetMapping("/upcoming_events")
    public String upcomingEventsLegacy() {
        return "redirect:/events/upcoming";
    }

    @GetMapping("/past_events")
    public String pastEventsLegacy() {
        return "redirect:/events/past";
    }
}
