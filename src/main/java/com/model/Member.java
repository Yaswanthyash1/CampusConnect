package com.model;

import jakarta.persistence.*;

@Entity
public class Member {
    @Id
    private String srn;
    private String name;
    private String email;
    private String domain;
    private int sem;
    private String dept;
    private String phoneno;
    private String gender;
    private String password;

    // Getters and setters
    public String getSrn() { return srn; }
    public void setSrn(String srn) { this.srn = srn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public int getSem() { return sem; }
    public void setSem(int sem) { this.sem = sem; }
    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }
    public String getPhoneno() { return phoneno; }
    public void setPhoneno(String phoneno) { this.phoneno = phoneno; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
