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
        String role = (String) requestBody.get("role");
        if (role == null || role.trim().isEmpty()) {
            // Fallback: use userType if present
            Object userTypeObj = requestBody.get("userType");
            if (userTypeObj != null) {
                role = userTypeObj.toString();
            }
        }
        if ("member".equalsIgnoreCase(role)) {
            String srn = (String) requestBody.get("srn");
            String username = (String) requestBody.get("username");
            // For members, if username is null, use srn as username
            if (username == null || username.trim().isEmpty()) {
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
                    ? Integer.valueOf(requestBody.get("sem").toString()) : null,
                (String) requestBody.get("dept"),
                (String) requestBody.get("phoneno"),
                (String) requestBody.get("gender"),
                (String) requestBody.get("club")
            );
        } else if ("club".equalsIgnoreCase(role)) {
            // Do not register clubs in the user table
            System.out.println("Club registration: skipping user table insert.");
        } else {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
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
            java.util.List<User> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
            if (users.isEmpty()) {
                return null;
            }
            return users.get(0);
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
            String clubDbUrl = "jdbc:mysql://host.docker.internal:3306/clubdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl(clubDbUrl);
            dataSource.setUsername("clubuser");
            dataSource.setPassword("clubpass");
            org.springframework.jdbc.core.JdbcTemplate clubJdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            String sql = "SELECT COUNT(*) FROM club WHERE clubName = ? AND password = ?";
            System.out.println("Executing SQL: " + sql + " with clubName='" + identifier + "', password='" + password + "'");
            Integer count = clubJdbcTemplate.queryForObject(sql, Integer.class, identifier, password);
            System.out.println("Club validation query result count: " + count);
            System.out.println("Club validation result: " + (count != null && count > 0));
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error validating club: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void saveUser(User user) {
        try {
            System.out.println("Attempting to save user: ID=" + user.getId() + ", SRN=" + user.getSrn() + ", Role=" + user.getRole() + ", Club=" + user.getClub());

            // Update user regardless of role (since role might be null for existing users)
            // Use ID for update if available, otherwise use SRN
            if (user.getId() != null) {
                String sql = "UPDATE user SET name = ?, email = ?, password = ?, role = ?, domain = ?, sem = ?, dept = ?, phoneno = ?, gender = ?, club = ? WHERE id = ?";
                int rowsAffected = jdbcTemplate.update(sql,
                    user.getName(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole(),
                    user.getDomain(),
                    user.getSem(),
                    user.getDept(),
                    user.getPhoneno(),
                    user.getGender(),
                    user.getClub(),
                    user.getId());
                System.out.println("Updated user by ID. Rows affected: " + rowsAffected);
            } else if (user.getSrn() != null) {
                String sql = "UPDATE user SET name = ?, email = ?, password = ?, role = ?, domain = ?, sem = ?, dept = ?, phoneno = ?, gender = ?, club = ? WHERE srn = ?";
                int rowsAffected = jdbcTemplate.update(sql,
                    user.getName(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole(),
                    user.getDomain(),
                    user.getSem(),
                    user.getDept(),
                    user.getPhoneno(),
                    user.getGender(),
                    user.getClub(),
                    user.getSrn());
                System.out.println("Updated user by SRN. Rows affected: " + rowsAffected);
            } else {
                System.err.println("Cannot save user: both ID and SRN are null");
            }
        } catch (Exception e) {
            System.err.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User findById(Long id) {
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
            }, id);
        } catch (Exception e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            return null;
        }
    }

    public java.util.List<User> findUsersByClub(String clubName) {
        String sql = "SELECT * FROM user WHERE club = ?";
        System.out.println("DEBUG: Finding users by club: '" + clubName + "'");
        System.out.println("DEBUG: SQL Query: " + sql);
        try {
            java.util.List<User> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
                System.out.println("DEBUG: Found user: " + user.getName() + " (SRN: " + user.getSrn() + ", Club: " + user.getClub() + ")");
                return user;
            }, clubName);
            System.out.println("DEBUG: Total users found for club '" + clubName + "': " + users.size());
            return users;
        } catch (Exception e) {
            System.err.println("Error finding users by club: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
}
