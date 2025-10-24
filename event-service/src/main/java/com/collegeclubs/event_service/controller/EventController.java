package com.collegeclubs.event_service.controller;

import com.collegeclubs.event_service.model.Event;
import com.collegeclubs.event_service.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class EventController {

    @Autowired
    private EventService eventService;


    @GetMapping("/event-details/{id}")
    public String showEventDetails(@PathVariable("id") Long id, Model model) {
        Event event = eventService.getEventById(id);
        model.addAttribute("event", event);
        // determine whether the event is upcoming (timestamp after now)
        boolean isUpcoming = false;
        if (event != null && event.getTimestamp() != null) {
            Timestamp now = Timestamp.from(Instant.now());
            isUpcoming = event.getTimestamp().after(now);
        }
        model.addAttribute("isUpcoming", isUpcoming);
        return "event-details";
    }

    // REST API endpoints for frontend integration
    @GetMapping("/events/upcoming")
    @ResponseBody
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        try {
            List<Event> upcomingEvents = eventService.getUpcomingEvents();
            return ResponseEntity.ok(upcomingEvents);
        } catch (Exception e) {
            System.err.println("Error fetching upcoming events: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/events/past")
    @ResponseBody
    public ResponseEntity<List<Event>> getPastEvents() {
        try {
            List<Event> pastEvents = eventService.getPastEvents();
            return ResponseEntity.ok(pastEvents);
        } catch (Exception e) {
            System.err.println("Error fetching past events: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/events/all")
    @ResponseBody
    public ResponseEntity<List<Event>> getAllEventsApi() {
        try {
            List<Event> events = eventService.getAllEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            System.err.println("Error fetching all events: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Create Event (REST) - consumed by frontend service via multipart/form-data
    @PostMapping(value = "/api/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> createEvent(@RequestParam("clubname") String clubName,
                                         @RequestParam("eventname") String eventName,
                                         @RequestParam("description") String description,
                                         @RequestParam("location") String location,
                                         @RequestParam("type") String type,
                                         @RequestParam("timestamp") String timestamp,
                                         @RequestParam("budget") double budget,
                                         @RequestParam("registrationlink") String registrationLink,
                                         @RequestParam(value = "banner", required = false) MultipartFile banner,
                                         @RequestParam(value = "fromRequest", required = false) String fromRequest) {
        try {
            Event event = new Event();
            event.setClubName(clubName);
            event.setEventName(eventName);
            event.setDescription(description);
            event.setVenue(location);
            event.setType(type);

            // Parse timestamp from datetime-local input (format: yyyy-MM-dd'T'HH:mm)
            LocalDateTime localDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            event.setTimestamp(Timestamp.valueOf(localDateTime));

            event.setBudget(budget);
            event.setRegistrationLink(registrationLink);

            if (banner != null && !banner.isEmpty()) {
                event.setBanner(banner.getBytes());
            }

            Event saved = eventService.saveEvent(event);

            return ResponseEntity.ok(saved.getId());
        } catch (Exception e) {
            System.err.println("Error creating event (REST): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating event");
        }
    }
}