#!/usr/bin/env bash
# Backs up the database and the uploaded pictures (plants and review photos) of the compose deployment (docs/DEPLOY.md).
#
#   scripts/backup.sh [backup directory]     default: ./backups
#
# Writes db-<time>.dump (pg_dump custom format) and storage-<time>.tar.gz, then deletes local
# backups older than KEEP_DAYS (default 14). If BACKUP_REMOTE is set (an rclone remote such as
# "offsite:shobuj-backups"), both files are copied there too.
set -euo pipefail

cd "$(dirname "$0")/.."
DIR="${1:-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
COMPOSE=(docker compose -f deploy/compose.yaml --env-file deploy/.env)

mkdir -p "$DIR"
chmod 700 "$DIR"

# A consistent snapshot while the shop keeps running.
"${COMPOSE[@]}" exec -T db pg_dump -U shobuj -d shobuj --format=custom --no-owner > "$DIR/db-$STAMP.dump"
# Plant pictures uploaded in the admin and customers' review photos.
"${COMPOSE[@]}" exec -T backend tar czf - -C /var/lib/shobuj/storage . > "$DIR/storage-$STAMP.tar.gz"

# An empty or unreadable dump is worse than none: check it lists the orders table. (Not grep -q:
# it stops reading at the first match, and pg_restore's broken pipe would then fail the check.)
"${COMPOSE[@]}" exec -T db pg_restore --list < "$DIR/db-$STAMP.dump" | grep "TABLE public orders" > /dev/null \
  || { echo "Backup check failed: $DIR/db-$STAMP.dump has no orders table" >&2; exit 1; }
chmod 600 "$DIR/db-$STAMP.dump" "$DIR/storage-$STAMP.tar.gz"

if [[ -n "${BACKUP_REMOTE:-}" ]]; then
  rclone copy "$DIR/db-$STAMP.dump" "$BACKUP_REMOTE"
  rclone copy "$DIR/storage-$STAMP.tar.gz" "$BACKUP_REMOTE"
fi

find "$DIR" -name 'db-*.dump' -mtime +"$KEEP_DAYS" -delete
find "$DIR" -name 'storage-*.tar.gz' -mtime +"$KEEP_DAYS" -delete
echo "Backed up to $DIR/db-$STAMP.dump and $DIR/storage-$STAMP.tar.gz"
