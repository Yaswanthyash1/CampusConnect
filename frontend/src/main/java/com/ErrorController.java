package com;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model,
            @RequestParam(required = false) String message) {

        Object status = request.getAttribute("javax.servlet.error.status_code");

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        } else {
            model.addAttribute("message", "An unexpected error occurred");
        }

        if (status != null) {
            model.addAttribute("status", status);
        } else {
            model.addAttribute("status", "Unknown");
        }

        return "error";
    }
}
