//package com.controller;
//
//import com.model.Event;
//import com.service.EventService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.util.List;
//
//@Controller
//public class EventPageController {
//    @Autowired
//    private EventService eventService;
//
//    @GetMapping("/event")
//    public String showEventPage(Model model) {
//        List<Event> events = eventService.getAllEvents();
//        model.addAttribute("events", events);
//        return "event";
//    }
//}
