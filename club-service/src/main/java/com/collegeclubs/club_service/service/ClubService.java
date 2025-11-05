package com.collegeclubs.club_service.service;

import com.collegeclubs.club_service.model.Club;
import com.collegeclubs.club_service.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class ClubService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ClubRepository clubRepository;

    private String clubMicroserviceUrl;

    public Club getClubDetailsByName(String clubName) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs/" + clubName;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);

            if (result == null) {
                return null;
            }

            Club club = new Club();
            club.setId(((Number) result.get("id")).longValue());
            club.setClubName((String) result.get("clubName"));
            club.setDescription((String) result.get("description"));
            club.setFacultyId((String) result.get("faculty_id"));
            club.setClubType((String) result.get("clubType"));
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

    /**
     * Register a new club from Map data
     * 
     * @param clubData Map containing club data
     * @return Saved Club entity
     */
    public Club registerClub(Map<String, Object> clubData) {
        try {
            System.out.println("ClubService processing club registration data: " + clubData);

            Club club = new Club();

            // Directly get the clubName from clubData - this is what the form sends
            String clubName = (String) clubData.get("clubName");

            // If not found, try to find it in other possible fields
            if (clubName == null) {
                if (clubData.get("club") != null) {
                    clubName = (String) clubData.get("club");
                    System.out.println("Found club: " + clubName);
                } else {
                    // Last resort - try to extract from the keys
                    for (String key : clubData.keySet()) {
                        if (key.toLowerCase().contains("club") && !key.equals("clubType")) {
                            clubName = (String) clubData.get(key);
                            System.out.println("Found clubName in key " + key + ": " + clubName);
                            break;
                        }
                    }
                }
            }

            System.out.println("ClubService setting clubName to: " + clubName);
            club.setClubName(clubName);

            // Set other fields directly
            club.setDescription((String) clubData.get("description"));
            club.setFacultyId((String) clubData.get("facultyId"));
            club.setClubType((String) clubData.get("clubType"));
            club.setHeadSrn((String) clubData.get("headSrn"));
            club.setName((String) clubData.get("name"));

            // Handle sem field
            Object semObj = clubData.get("sem");
            if (semObj instanceof String) {
                club.setSem(Integer.parseInt((String) semObj));
            } else if (semObj instanceof Integer) {
                club.setSem((Integer) semObj);
            } else if (semObj instanceof Number) {
                club.setSem(((Number) semObj).intValue());
            }

            club.setDept((String) clubData.get("dept"));
            club.setPhoneno((String) clubData.get("phoneno"));
            club.setGender((String) clubData.get("gender"));
            club.setPassword((String) clubData.get("password"));

            // Save the club entity
            Club savedClub = clubRepository.save(club);
            System.out.println("ClubService saved club successfully with ID: " + savedClub.getId());

            // Insert club credentials into clubdb.club for login compatibility
            try {
                String clubDbUrl = "jdbc:mysql://host.docker.internal:3306/clubdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
                dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                dataSource.setUrl(clubDbUrl);
                dataSource.setUsername("clubuser");
                dataSource.setPassword("clubpass");
                org.springframework.jdbc.core.JdbcTemplate clubJdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
                String sql = "INSERT INTO club (clubName, password) VALUES (?, ?) ON DUPLICATE KEY UPDATE password = VALUES(password)";
                clubJdbcTemplate.update(sql, club.getClubName(), club.getPassword());
                System.out.println("Inserted/updated club credentials in clubdb.club for login");
            } catch (Exception e) {
                System.err.println("Error inserting club credentials into clubdb.club: " + e.getMessage());
            }

            return savedClub;
        } catch (Exception e) {
            System.err.println("Error in ClubService.registerClub: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Club createClub(String clubName, String description, String password, String facultyId,
            String clubType, String headSrn, String name, Integer sem,
            String dept, String phoneno, String gender) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs";

            Map<String, Object> clubData = new HashMap<>();
            clubData.put("clubName", clubName);
            clubData.put("description", description);
            clubData.put("facultyId", facultyId);
            clubData.put("clubType", clubType);
            clubData.put("headSrn", headSrn);
            clubData.put("name", name);
            clubData.put("sem", sem);
            clubData.put("dept", dept);
            clubData.put("phoneno", phoneno);
            clubData.put("gender", gender);
            clubData.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(clubData, headers);

            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);

            if (result != null) {
                return getClubDetailsByName(clubName);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Club findByClubNameAndPassword(String clubName, String password) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs/" + clubName + "/authenticate?password=" + password;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);

            if (result == null) {
                return null;
            }

            Club club = new Club();
            club.setId(((Number) result.get("id")).longValue());
            club.setClubName((String) result.get("clubName"));
            club.setDescription((String) result.get("description"));
            club.setFacultyId((String) result.get("faculty_id"));
            club.setClubType((String) result.get("clubType"));
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

    public List<Map<String, Object>> getClubsByFaculty(int facultyId) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs/faculty/" + facultyId;
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getClubMembers(String clubName) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs/" + clubName + "/members";
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getClubNameByHeadSrn(String headSrn) {
        try {
            String url = clubMicroserviceUrl + "/api/clubs/head/" + headSrn;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result != null && result.containsKey("clubName")) {
                return (String) result.get("clubName");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
