package com.model;

import jakarta.persistence.*;

import java.nio.file.Paths;
import java.sql.Timestamp;

@Entity
@Table(name = "member_request")
public class MemberRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "type")
    private String type; // idea, query, proposal

    @Column(name = "description")
    private String description;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "status")
    private String status = "pending"; // pending, accepted, rejected

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Column(name = "club_name")
    private String clubName; // optional: the club this request targets (for club join requests)

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = new Timestamp(System.currentTimeMillis());
        }
        if (this.status == null) {
            this.status = "pending";
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    // Convenience method to return only the filename portion of filePath
    public String getFileName() {
        if (this.filePath == null) return "";
        try {
            return Paths.get(this.filePath).getFileName().toString();
        } catch (Exception e) {
            return this.filePath;
        }
    }
}
