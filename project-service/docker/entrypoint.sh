#!/usr/bin/env sh
set -e
SOCK=/run/mysqld/mysqld.sock
DATADIR=/var/lib/mysql
INIT_MARKER="$DATADIR/.init_done"
mkdir -p "$DATADIR" /run/mysqld
chown -R mysql:mysql "$DATADIR" /run/mysqld || true
if [ ! -d "$DATADIR/mysql" ]; then
  echo "Initializing MariaDB data directory..."
  if command -v mariadb-install-db >/dev/null 2>&1; then mariadb-install-db --user=mysql --datadir="$DATADIR"; elif command -v mysql_install_db >/dev/null 2>&1; then mysql_install_db --user=mysql --datadir="$DATADIR"; fi
fi
if command -v mysqld >/dev/null 2>&1; then
  mysqld --user=mysql --datadir="$DATADIR" --socket="$SOCK" --bind-address=0.0.0.0 &
else
  mariadbd --user=mysql --datadir="$DATADIR" --socket="$SOCK" --bind-address=0.0.0.0 &
fi
for i in $(seq 1 90); do if mysqladmin --protocol=SOCKET --socket="$SOCK" ping --silent; then break; fi; sleep 1; echo "Waiting for MariaDB socket ($i)..."; done
if [ ! -f "$INIT_MARKER" ] && [ -f /docker-entrypoint-initdb.d/init.sql ]; then
  echo "Running initial schema setup..."
  mysql --protocol=SOCKET --socket="$SOCK" -uroot -e "FLUSH PRIVILEGES;" || true
  mysql --protocol=SOCKET --socket="$SOCK" -uroot < /docker-entrypoint-initdb.d/init.sql || true
  touch "$INIT_MARKER"
fi
exec sh -c "java $JAVA_OPTS -jar /app/app.jar"

