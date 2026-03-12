#!/bin/bash
#
# MySQL DB 백업 스크립트
# - Docker MySQL 컨테이너에서 덤프 생성
# - gzip 압축 후 S3 업로드
# - Discord scheduler 채널 알림 (성공/실패)
#
# 사용법:
#   ./backup-db.sh                    # 기본 실행
#   ./backup-db.sh --local-only       # S3 업로드 없이 로컬만 저장
#
# Cron 설정 예시 (매일 새벽 3시):
#   0 3 * * * /path/to/scripts/backup-db.sh >> /path/to/scripts/logs/db-backup.log 2>&1
#   → setup-cron.sh 실행 시 자동 등록됨
#
# 환경변수 (.env에서 설정):
#   DB_PASSWORD                DB 비밀번호 (필수)
#   AWS_S3_BUCKET              S3 버킷명 (필수)
#   DB_USERNAME                DB 유저 (기본: root)
#   CONTAINER_NAME             MySQL 컨테이너명 (기본: codemate-mysql)
#   DB_NAME                    DB명 (기본: codemate)
#   BACKUP_DIR                 로컬 백업 디렉토리 (기본: scripts/backups)
#   DISCORD_WEBHOOK_SCHEDULER  Discord webhook URL (없으면 알림 생략)
#

set -euo pipefail

# Amazon Linux 2/2023: aws cli v2가 /usr/local/bin에 있음
export PATH=/usr/local/bin:/usr/bin:/bin

# .env 파일 로드 (S3_BUCKET, DISCORD_WEBHOOK_SCHEDULER 등)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -f "${HOME}/.env" ]]; then
    # export하여 하위 프로세스(aws, curl)에도 전달
    set -a
    # shellcheck source=/dev/null
    source "${HOME}/.env"
    set +a
fi

# ============================================
# 설정
# ============================================
CONTAINER_NAME="${CONTAINER_NAME:-codemate-mysql}"
DB_NAME="${DB_NAME:-codemate}"
DB_USER="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:?".env에 DB_PASSWORD가 설정되지 않았습니다."}"

# S3 설정 (환경변수로 오버라이드 가능)
S3_BUCKET="${AWS_S3_BUCKET:-your-bucket-name}"

# 로컬 백업 디렉토리
BACKUP_DIR="${BACKUP_DIR:-$(dirname "$0")/backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="codemate_${TIMESTAMP}.sql.gz"

# 보존 설정
LOCAL_RETENTION_DAYS=1   # 로컬에 보관할 일수 (S3는 수명 주기 규칙으로 30일 관리)

# Discord Webhook
DISCORD_WEBHOOK="${DISCORD_WEBHOOK_SCHEDULER:-}"

# 시작 시각 기록
START_TIME=$(date +%s)

# ============================================
# 함수
# ============================================
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

elapsed_seconds() {
    echo $(( $(date +%s) - START_TIME ))
}

discord_notify() {
    local color="$1"   # 65280=성공(초록), 16711680=실패(빨강)
    local title="$2"
    local description="$3"

    if [[ -z "${DISCORD_WEBHOOK}" ]]; then
        return
    fi

    curl -s -X POST "${DISCORD_WEBHOOK}" \
        -H "Content-Type: application/json" \
        -d "{\"embeds\":[{\"title\":\"${title}\",\"description\":\"${description}\",\"color\":${color}}]}" \
        > /dev/null
}

FAIL_REASON=""

error_exit() {
    log "ERROR: $1"
    FAIL_REASON="$1"
    exit 1
}

cleanup() {
    local exit_code=$?

    if [[ -f "/tmp/${BACKUP_FILE}" ]]; then
        rm -f "/tmp/${BACKUP_FILE}"
    fi

    if [[ ${exit_code} -ne 0 ]]; then
        local reason="${FAIL_REASON:-예상치 못한 오류 (exit ${exit_code})}"
        discord_notify 16711680 \
            "❌ DB 백업 실패" \
            "**에러:** ${reason}\n**경과:** $(elapsed_seconds)초"
    fi
}

trap cleanup EXIT

# ============================================
# 사전 체크
# ============================================
log "DB 백업 시작"

# Docker 컨테이너 실행 확인
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    error_exit "MySQL 컨테이너 '${CONTAINER_NAME}'가 실행중이 아닙니다."
fi

# 백업 디렉토리 생성
mkdir -p "${BACKUP_DIR}"

# ============================================
# 백업 실행
# ============================================
log "MySQL 덤프 생성 중..."

# MYSQL_PWD 환경변수로 전달: 커맨드라인에 비밀번호 노출 방지 (ps aux 등)
docker exec -e MYSQL_PWD="${DB_PASSWORD}" "${CONTAINER_NAME}" mysqldump \
    -u"${DB_USER}" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    "${DB_NAME}" 2>/dev/null | gzip > "/tmp/${BACKUP_FILE}"

if [[ ! -s "/tmp/${BACKUP_FILE}" ]]; then
    error_exit "백업 파일 생성 실패 (파일이 비어있음)"
fi

chmod 600 "/tmp/${BACKUP_FILE}"

BACKUP_SIZE=$(du -h "/tmp/${BACKUP_FILE}" | cut -f1)
log "덤프 완료: ${BACKUP_FILE} (${BACKUP_SIZE})"

# 로컬에 복사
cp "/tmp/${BACKUP_FILE}" "${BACKUP_DIR}/${BACKUP_FILE}"
chmod 600 "${BACKUP_DIR}/${BACKUP_FILE}"
log "로컬 저장 완료: ${BACKUP_DIR}/${BACKUP_FILE}"

# ============================================
# S3 업로드 (--local-only 옵션이 없을 때만)
# ============================================
if [[ "${1:-}" != "--local-only" ]]; then
    if command -v aws &> /dev/null; then
        # SES용 IAM 키가 .env에 있으면 EC2 인스턴스 롤 대신 사용됨 → unset하여 인스턴스 롤로 인증
        unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
        log "S3 업로드 중... (s3://${S3_BUCKET}/)"

        if aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE}" "s3://${S3_BUCKET}/${BACKUP_FILE}"; then
            log "S3 업로드 완료"
        else
            log "WARNING: S3 업로드 실패 (로컬 백업은 유지됨)"
        fi
    else
        log "WARNING: AWS CLI가 설치되지 않음 - S3 업로드 생략"
    fi
fi

# ============================================
# 오래된 로컬 백업 정리
# ============================================
log "오래된 로컬 백업 정리 중 (${LOCAL_RETENTION_DAYS}일 이상)..."
DELETED_COUNT=$(find "${BACKUP_DIR}" -name "codemate_*.sql.gz" -type f -mtime +${LOCAL_RETENTION_DAYS} -delete -print | wc -l | tr -d ' ')

if [[ ${DELETED_COUNT} -gt 0 ]]; then
    log "삭제된 로컬 백업: ${DELETED_COUNT}개"
fi


# ============================================
# 완료 및 Discord 알림
# ============================================
ELAPSED=$(elapsed_seconds)
S3_PATH=""
if [[ "${1:-}" != "--local-only" ]] && command -v aws &> /dev/null; then
    S3_PATH="s3://${S3_BUCKET}/${BACKUP_FILE}"
fi

log "백업 완료! (${ELAPSED}초)"
log "  - 로컬: ${BACKUP_DIR}/${BACKUP_FILE} (${BACKUP_SIZE})"
[[ -n "${S3_PATH}" ]] && log "  - S3: ${S3_PATH}"

DISCORD_DESC="**파일:** \`${BACKUP_FILE}\`\n**크기:** ${BACKUP_SIZE}\n**경과:** ${ELAPSED}초"
[[ -n "${S3_PATH}" ]] && DISCORD_DESC="${DISCORD_DESC}\n**S3:** \`${S3_PATH}\`"

discord_notify 65280 "✅ DB 백업 성공" "${DISCORD_DESC}"

exit 0