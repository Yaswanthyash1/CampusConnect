package com.collegeclubs.club_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "club")
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clubName")
    private String clubName;

    @Column(name = "description")
    private String description;

    @Column(name = "faculty_id")
    private String facultyId;

    @Column(name = "clubType")
    private String clubType;

    @Column(name = "head_srn")
    private String headSrn;

    @Column(name = "name")
    private String name;

    @Column(name = "phoneno")
    private String phoneno;

    @Column(name = "dept")
    private String dept;

    @Column(name = "gender")
    private String gender;

    @Column(name = "sem")
    private Integer sem;

    @Column(name = "password")
    private String password;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getClubType() {
        return clubType;
    }

    public void setClubType(String clubType) {
        this.clubType = clubType;
    }

    public String getHeadSrn() {
        return headSrn;
    }

    public void setHeadSrn(String headSrn) {
        this.headSrn = headSrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public Integer getSem() {
        return sem;
    }

    public void setSem(Integer sem) {
        this.sem = sem;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
