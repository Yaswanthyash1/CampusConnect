# 🚀 Dockerizing CampusConnect (6 Containers, Each with Its Own Database)

This setup runs **6 Spring Boot containers**.  
Each container embeds its own **MariaDB** instance and initializes its schema at startup, based on `sql-workbench` contents (split per service).

---

## 🧩 Services and Ports

| Service           | Application Port | Internal MariaDB Port |
|-------------------|------------------|------------------------|
| `frontend`        | 8080             | 3306                   |
| `user-service`    | 8081             | 3306                   |
| `club-service`    | 8082             | 3306                   |
| `request-service` | 8083             | 3306                   |
| `project-service` | 8084             | 3306                   |
| `event-service`   | 8085             | 3306                   |

---

## 💾 Data Persistence

- Each service mounts a **named Docker volume** at `/var/lib/mysql`.
- This ensures **database data survives** container restarts or rebuilds.

---

## ☁️ GCP VM Setup

### 🖥️ Create VM

- Use **Ubuntu 22.04 LTS**
- Allow **HTTP** and **HTTPS** traffic
- Recommended configuration: **16 GB RAM**, **60 GB disk**
- Please open the ports in GCP firewall settings:
  - `8080-8085` for application access
  - `22` for SSH

---

### 🐳 Install Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

sudo systemctl enable docker
sudo systemctl start docker

docker --version
```
### 🔐 Setup SSH Keys for GitHub
```bat
ssh-keygen -t ed25519 -C "your_email@example.com"
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
```
- Add the displayed public key to your GitHub account under **Settings > SSH and GPG keys**.

```bat
git clone https://github.com/Yaswanthyash1/CampusConnect.git
git checkout multi-docker
docker compose build
docker compose up -d
```

### 🚀 Deploy CampusConnect

```bash
REM Check status/logs
docker compose ps
docker compose logs -f user-service
```

### Notes
- Each service’s `application.properties` points to `jdbc:mysql://localhost:3306/<service_db>` which resolves to the MariaDB running in the same container.
- Initialization SQL per service is in `<service>/docker/mysql-init.sql`, derived from `sql-workbench` (tables and grants). Users are created as 'clubuser'@'localhost' and 'clubuser'@'%' for robustness.
- For local development without containers, the previous compose with host MySQL is in git history if needed.
- Health checks are not added to keep things simple; consider adding service-level health endpoints later.
