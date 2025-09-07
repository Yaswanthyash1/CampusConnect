package com.collegeclubs.user_service.service;

import com.collegeclubs.user_service.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void registerUser(String srn, String username, String name, String email, String password, String role,
            String domain, Integer sem, String dept, String phoneno, String gender, String club) {
        System.out.println("Registering user with SRN: " + srn);
        try {
            String sql = "INSERT INTO user (srn, username, name, email, password, role, domain, sem, dept, phoneno, gender, club) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, srn, username, name, email, password, role, domain, sem, dept, phoneno, gender,
                    club);
            System.out.println("User registered successfully: " + srn);
        } catch (Exception e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void register(Map<String, Object> requestBody) {
        String srn = (String) requestBody.get("srn");
        String username = (String) requestBody.get("username");
        String role = (String) requestBody.get("role");

        // For members, if username is null, use srn as username
        if ("member".equalsIgnoreCase(role) && (username == null || username.trim().isEmpty())) {
            username = srn;
        }

        registerUser(
                srn,
                username,
                (String) requestBody.get("name"),
                (String) requestBody.get("email"),
                (String) requestBody.get("password"),
                role,
                (String) requestBody.get("domain"),
                requestBody.get("sem") != null && !requestBody.get("sem").toString().isEmpty()
                        ? Integer.parseInt(requestBody.get("sem").toString())
                        : null,
                (String) requestBody.get("dept"),
                (String) requestBody.get("phoneno"),
                (String) requestBody.get("gender"),
                (String) requestBody.get("club"));

        //
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setSrn(rs.getString("srn"));
                user.setUsername(rs.getString("username"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setDomain(rs.getString("domain"));
                user.setSem(rs.getObject("sem", Integer.class));
                user.setDept(rs.getString("dept"));
                user.setPhoneno(rs.getString("phoneno"));
                user.setGender(rs.getString("gender"));
                user.setClub(rs.getString("club"));
                return user;
            }, username);
        } catch (Exception e) {
            System.err.println("Error finding user by username: " + e.getMessage());
            return null;
        }
    }

    public User findBySrn(String srn) {
        String sql = "SELECT * FROM user WHERE srn = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setSrn(rs.getString("srn"));
                user.setUsername(rs.getString("username"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setDomain(rs.getString("domain"));
                user.setSem(rs.getObject("sem", Integer.class));
                user.setDept(rs.getString("dept"));
                user.setPhoneno(rs.getString("phoneno"));
                user.setGender(rs.getString("gender"));
                user.setClub(rs.getString("club"));
                return user;
            }, srn);
        } catch (Exception e) {
            System.err.println("Error finding user by SRN: " + e.getMessage());
            return null;
        }
    }

    public boolean validateUser(String username, String password) {
        String sql = "SELECT COUNT(*) FROM user WHERE srn = ? AND password = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, password);
        return count != null && count > 0;
    }

    public boolean validateFaculty(String identifier, String password) {
        String sql = "SELECT COUNT(*) FROM faculty WHERE email = ? AND password = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, identifier, password);
        return count != null && count > 0;
    }

    public boolean validateClub(String identifier, String password) {
        System.out.println("Validating club login for: " + identifier + " with password: " + password);
        try {
            // Connect directly to clubdb for club validation
            String clubDbUrl = "jdbc:mysql://localhost:3306/clubdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

            // Create a separate JdbcTemplate for clubdb
            org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl(clubDbUrl);
            dataSource.setUsername("clubuser");
            dataSource.setPassword("clubpass");

            org.springframework.jdbc.core.JdbcTemplate clubJdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(
                    dataSource);

            // Query clubdb.club table
            String sql = "SELECT COUNT(*) FROM club WHERE clubName = ? AND password = ?";
            Integer count = clubJdbcTemplate.queryForObject(sql, Integer.class, identifier, password);

            System.out.println("Club validation result: " + (count != null && count > 0));
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error validating club: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void saveUser(User user) {
        // if user type is member, then check srn and update all the fields in user
        // table
        if ("member".equalsIgnoreCase(user.getRole())) {
            String sql = "UPDATE user SET name = ?, email = ?, password = ?, domain = ?, sem = ?, dept = ?, phoneno = ?, gender = ?, club = ? WHERE srn = ?";
            jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getPassword(), user.getDomain(),
                    user.getSem(), user.getDept(),
                    user.getPhoneno(), user.getGender(), user.getClub(), user.getSrn());
        }
    }

    public User findById(Long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setSrn(rs.getString("srn"));
                user.setUsername(rs.getString("username"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setDomain(rs.getString("domain"));
                user.setSem(rs.getObject("sem", Integer.class));
                user.setDept(rs.getString("dept"));
                user.setPhoneno(rs.getString("phoneno"));
                user.setGender(rs.getString("gender"));
                user.setClub(rs.getString("club"));
                return user;
            }, id);
        } catch (Exception e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            return null;
        }
    }

    public java.util.List<User> findUsersByClub(String clubName) {
        String sql = "SELECT * FROM user WHERE club = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setSrn(rs.getString("srn"));
                user.setUsername(rs.getString("username"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                // Don't include password for security reasons
                user.setRole(rs.getString("role"));
                user.setDomain(rs.getString("domain"));
                user.setSem(rs.getObject("sem", Integer.class));
                user.setDept(rs.getString("dept"));
                user.setPhoneno(rs.getString("phoneno"));
                user.setGender(rs.getString("gender"));
                user.setClub(rs.getString("club"));
                return user;
            }, clubName);
        } catch (Exception e) {
            System.err.println("Error finding users by club: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
