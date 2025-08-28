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
        return jdbcTemplate.query(sql, new EventRowMapper());
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

