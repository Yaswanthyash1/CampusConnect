package com.service;

import com.model.Club;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ClubService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Club getClubDetailsByName(String clubName) {
        String sql = "SELECT * FROM club WHERE clubName = ?";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, clubName);
            Club club = new Club();
            club.setId(((Number) result.get("id")).longValue());
            club.setClubName((String) result.get("clubName"));
            club.setDescription((String) result.get("description"));
            club.setFacultyId((String) result.get("faculty_id"));
            club.setClubType((String) result.get("club_type"));
            club.setHeadSrn((String) result.get("head_srn"));
            club.setName((String) result.get("name"));
            club.setSem(result.get("sem") != null ? Integer.parseInt(result.get("sem").toString()) : null);
            club.setDept((String) result.get("dept"));
            club.setPhoneno((String) result.get("phoneno"));
            club.setGender((String) result.get("gender"));
            club.setPassword((String) result.get("password"));
            return club;
        } catch (Exception e) {
            return null;
        }
    }

    public Club createClub(String clubName, String description, String password, String facultyId,
                           String clubType, String headSrn, String name, Integer sem,
                           String dept, String phoneno, String gender) {
        String sql = "INSERT INTO club (clubName, description, faculty_id, club_type, head_srn, name, sem, dept, phoneno, gender, password) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                clubName,
                description,
                facultyId,
                clubType,
                headSrn,
                name,
                sem,
                dept,
                phoneno,
                gender,
                password
        );
        // Fetch the newly created club
        return getClubDetailsByName(clubName);
    }

    public Club findByClubNameAndPassword(String clubName, String password) {
        String sql = "SELECT * FROM club WHERE clubName = ? AND password = ?";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, clubName, password);
            Club club = new Club();
            club.setId(((Number) result.get("id")).longValue());
            club.setClubName((String) result.get("clubName"));
            club.setDescription((String) result.get("description"));
            club.setFacultyId((String) result.get("faculty_id"));
            club.setClubType((String) result.get("club_type"));
            club.setHeadSrn((String) result.get("head_srn"));
            club.setName((String) result.get("name"));
            club.setSem(result.get("sem") != null ? Integer.parseInt(result.get("sem").toString()) : null);
            club.setDept((String) result.get("dept"));
            club.setPhoneno((String) result.get("phoneno"));
            club.setGender((String) result.get("gender"));
            club.setPassword((String) result.get("password"));
            return club;
        } catch (Exception e) {
            return null;
        }
    }

    public String getClubNameByHeadSrn(String headSrn) {
        String sql = "SELECT clubName FROM club WHERE head_srn = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, headSrn);
        } catch (Exception e) {
            return null;
        }
    }
}
