#!/bin/bash
#
# DB 백업 crontab 자동 등록 스크립트
# - 매일 새벽 3시 backup-db.sh 실행
# - 멱등성 보장: 이미 등록된 경우 중복 등록하지 않음
#
# 사용법:
#   ./setup-cron.sh
#
# 환경변수는 $HOME/.env 파일에서 로드됨 (backup-db.sh가 직접 source)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_SCRIPT="${SCRIPT_DIR}/backup-db.sh"
LOG_FILE="${SCRIPT_DIR}/logs/db-backup.log"
CRON_MARKER="# codemate-db-backup"  # 중복 감지용 고유 토큰

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 스크립트 존재 확인
if [[ ! -f "${BACKUP_SCRIPT}" ]]; then
    echo "ERROR: backup-db.sh를 찾을 수 없습니다: ${BACKUP_SCRIPT}"
    exit 1
fi

# .env 파일 존재 확인
if [[ ! -f "${HOME}/.env" ]]; then
    echo "WARNING: .env 파일이 없습니다: ${HOME}/.env"
    echo "DB_PASSWORD, S3_BUCKET, DISCORD_WEBHOOK_SCHEDULER 등을 .env에 설정하세요."
fi

chmod +x "${BACKUP_SCRIPT}"
mkdir -p "${SCRIPT_DIR}/logs"

# 이미 등록된 경우 스킵 (멱등성)
if crontab -l 2>/dev/null | grep -qF "${CRON_MARKER}"; then
    log "이미 crontab에 등록되어 있습니다."
    crontab -l | grep -F "${CRON_MARKER}"
    exit 0
fi

CRON_JOB="0 3 * * * ${BACKUP_SCRIPT} >> ${LOG_FILE} 2>&1 ${CRON_MARKER}"

(crontab -l 2>/dev/null; echo "${CRON_JOB}") | crontab -

log "crontab 등록 완료 (매일 새벽 3시):"
crontab -l | grep -F "${CRON_MARKER}"
log ""
log "로그 확인: tail -f ${LOG_FILE}"
log "수동 실행: ${BACKUP_SCRIPT}"
