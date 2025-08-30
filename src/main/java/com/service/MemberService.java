package com.service;

import com.model.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class MemberService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Member findBySrn(String srn) {
        String sql = "SELECT * FROM member WHERE srn = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{srn}, new MemberRowMapper());
        } catch (Exception e) {
            return null;
        }
    }

    public Member findByEmail(String email) {
        String sql = "SELECT * FROM member WHERE email = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{email}, new MemberRowMapper());
        } catch (Exception e) {
            return null;
        }
    }

    public Member findBySrnAndPassword(String srn, String password) {
        String sql = "SELECT * FROM member WHERE srn = ? AND password = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{srn, password}, new MemberRowMapper());
        } catch (Exception e) {
            return null;
        }
    }

    public Member createMember(String email, String password, String domain, String srn, String name, int sem, String dept, String phoneno, String gender) {
        String sql = "INSERT INTO member (srn, name, email, domain, sem, dept, phoneno, gender, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, srn, name, email, domain, sem, dept, phoneno, gender, password);
        return findBySrn(srn);
    }

    public void setMemberClub(String srn, String clubName) {
        String sql = "UPDATE member SET club = ? WHERE srn = ?";
        jdbcTemplate.update(sql, clubName, srn);
    }

    private static class MemberRowMapper implements RowMapper<Member> {
        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            Member member = new Member();
            member.setSrn(rs.getString("srn"));
            member.setName(rs.getString("name"));
            member.setEmail(rs.getString("email"));
            member.setDomain(rs.getString("domain"));
            member.setSem(rs.getInt("sem"));
            member.setDept(rs.getString("dept"));
            member.setPhoneno(rs.getString("phoneno"));
            member.setGender(rs.getString("gender"));
            member.setPassword(rs.getString("password"));
            member.setClub(rs.getString("club"));
            return member;
        }
    }
}
