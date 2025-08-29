package com.service;

import com.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class EventService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Event> getAllEvents() {
        String sql = "SELECT * FROM event";
        List<Event> events = jdbcTemplate.query(sql, new EventRowMapper());
        for (Event event : events) {
            if (event.getDescription() == null || event.getDescription().isEmpty()) {
                event.setDescriptionPreview("No description available.");
            } else if (event.getDescription().length() > 100) {
                event.setDescriptionPreview(event.getDescription().substring(0, 100) + "...");
            } else {
                event.setDescriptionPreview(event.getDescription());
            }
        }
        return events;
    }

    public Event getEventById(Long id) {
        String sql = "SELECT * FROM event WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new EventRowMapper(), id);
    }

    private static class EventRowMapper implements RowMapper<Event> {
        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            Event event = new Event();
            event.setId(rs.getLong("id"));
            event.setClubName(rs.getString("clubname"));
            event.setEventName(rs.getString("eventname"));
            event.setDescription(rs.getString("description"));
            event.setLocation(rs.getString("loc"));
            event.setType(rs.getString("type"));
            event.setTimestamp(rs.getTimestamp("timestamp"));
            event.setBudget(rs.getDouble("budget"));
            event.setRegistrationLink(rs.getString("registrationlink"));
            event.setBanner(rs.getBytes("banner"));
            return event;
        }
    }
}
