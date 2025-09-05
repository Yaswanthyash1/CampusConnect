package com.collegeclubs.user_service.model;

import jakarta.persistence.*;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String srn;
    private String username;
    private String name;
    private String email;
    private String password;
    private String role;
    private String domain;
    private Integer sem;
    private String dept;
    private String phoneno;
    private String gender;
    private String club;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSrn() { return srn; }
    public void setSrn(String srn) { this.srn = srn; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public Integer getSem() { return sem; }
    public void setSem(Integer sem) { this.sem = sem; }

    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }

    public String getPhoneno() { return phoneno; }
    public void setPhoneno(String phoneno) { this.phoneno = phoneno; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getClub() { return club; }
    public void setClub(String club) { this.club = club; }
}
