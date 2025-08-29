package com.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DataMigrationRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DataMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'requests'",
                    Integer.class);
            if (cnt == null || cnt == 0) {
                logger.info("Legacy table 'requests' not found. No migration needed.");
                return;
            }

            logger.info("Legacy table 'requests' found. Inspecting columns...");
            List<String> cols = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'requests'",
                    String.class);
            List<String> lowerCols = cols.stream().map(c -> c.toLowerCase(Locale.ROOT)).collect(Collectors.toList());

            // possible legacy column names
            String srnCol = null;
            for (String candidate : new String[]{"srn", "SRN", "member_id", "memberid", "memberId", "user_id"}) {
                if (lowerCols.contains(candidate.toLowerCase(Locale.ROOT))) {
                    // find original casing
                    final String match = cols.stream().filter(c -> c.equalsIgnoreCase(candidate)).findFirst().orElse(candidate);
                    srnCol = match;
                    break;
                }
            }

            String messageCol = null;
            for (String candidate : new String[]{"message", "msg", "description", "request", "request_text"}) {
                if (lowerCols.contains(candidate.toLowerCase(Locale.ROOT))) {
                    final String match = cols.stream().filter(c -> c.equalsIgnoreCase(candidate)).findFirst().orElse(candidate);
                    messageCol = match;
                    break;
                }
            }

            String clubCol = null;
            for (String candidate : new String[]{"clubname", "club_name", "club", "clubName"}) {
                if (lowerCols.contains(candidate.toLowerCase(Locale.ROOT))) {
                    final String match = cols.stream().filter(c -> c.equalsIgnoreCase(candidate)).findFirst().orElse(candidate);
                    clubCol = match;
                    break;
                }
            }

            logger.info("Detected legacy columns - srn: {}, message: {}, club: {}", srnCol, messageCol, clubCol);

            if (srnCol == null || messageCol == null) {
                logger.warn("Required legacy columns not found (srn/message). Skipping migration.");
                return;
            }

            // Build insert SQL using backticks around column names to preserve case
            String srnExpr = "`" + srnCol + "`";
            String messageExpr = "`" + messageCol + "`";
            String clubExpr = (clubCol == null) ? "NULL" : "`" + clubCol + "`";

            String insertSql = "INSERT INTO member_request (member_id, type, description, file_path, status, timestamp, club_name) \n" +
                    "SELECT " + srnExpr + ", 'join', " + messageExpr + ", NULL, 'pending', NOW(), " + clubExpr + " FROM requests r \n" +
                    "WHERE " + srnExpr + " IS NOT NULL AND TRIM(" + srnExpr + ") <> '' AND NOT EXISTS (SELECT 1 FROM member_request mr WHERE mr.member_id = r." + srnExpr + " AND mr.description = r." + messageExpr + " AND (mr.club_name = r." + (clubCol == null ? "NULL" : clubExpr) + " OR (mr.club_name IS NULL AND r." + (clubCol == null ? "NULL" : clubExpr) + " IS NULL)))";

            int migrated = jdbcTemplate.update(insertSql);
            logger.info("Data migration complete. Rows migrated: {}", migrated);

        } catch (Exception e) {
            logger.error("Data migration failed: {}", e.getMessage());
            logger.debug("Full exception", e);
        }
    }
}
