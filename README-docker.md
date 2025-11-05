# Dockerizing CampusConnect (6 containers, each with its own DB)

This setup runs 6 Spring Boot containers. Each container embeds its own MariaDB instance and initializes its schema at startup, based on `sql-workbench` contents (split per service).

Services and ports:
- frontend: 8080 (+ internal MariaDB on 3306)
- user-service: 8081 (+ internal MariaDB on 3306)
- club-service: 8082 (+ internal MariaDB on 3306)
- request-service: 8083 (+ internal MariaDB on 3306)
- project-service: 8084 (+ internal MariaDB on 3306)
- event-service: 8085 (+ internal MariaDB on 3306)

Data persistence
- Each service mounts a named Docker volume at `/var/lib/mysql` so DB data survives container restarts/rebuilds.

Build and run (Windows cmd):

```bat
ssh-keygen -t ed25519 -C "your_email@example.com"
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
```
Copy the output and add it to your GitHub account under SSH keys.

```bat
git clone https://github.com/Yaswanthyash1/CampusConnect.git
git checkout multi-docker
docker compose build
docker compose up -d

REM Check status/logs
docker compose ps
docker compose logs -f user-service
```

Notes
- Each service’s `application.properties` points to `jdbc:mysql://localhost:3306/<service_db>` which resolves to the MariaDB running in the same container.
- Initialization SQL per service is in `<service>/docker/mysql-init.sql`, derived from `sql-workbench` (tables and grants). Users are created as 'clubuser'@'localhost' and 'clubuser'@'%' for robustness.
- For local development without containers, the previous compose with host MySQL is in git history if needed.
- Health checks are not added to keep things simple; consider adding service-level health endpoints later.
