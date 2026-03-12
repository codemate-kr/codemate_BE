#!/bin/bash
#
# MySQL DB 복원 스크립트
# - 로컬 또는 S3에서 백업 파일을 가져와 복원
#
# 사용법:
#   ./restore-db.sh                              # 최신 로컬 백업으로 복원
#   ./restore-db.sh ./backups/codemate_xxx.sql.gz  # 특정 파일로 복원
#   ./restore-db.sh --from-s3 codemate_xxx.sql.gz  # S3에서 다운로드 후 복원
#   ./restore-db.sh --list                       # 로컬 백업 목록 조회
#   ./restore-db.sh --list-s3                    # S3 백업 목록 조회
#

set -euo pipefail

export PATH=/usr/local/bin:/usr/bin:/bin

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -f "${HOME}/.env" ]]; then
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

S3_BUCKET="${AWS_S3_BUCKET:-your-bucket-name}"

BACKUP_DIR="${BACKUP_DIR:-$(dirname "$0")/backups}"

# ============================================
# 함수
# ============================================
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

show_usage() {
    echo "사용법:"
    echo "  $0                              # 최신 로컬 백업으로 복원"
    echo "  $0 <backup-file.sql.gz>         # 특정 파일로 복원"
    echo "  $0 --from-s3 <filename>         # S3에서 다운로드 후 복원"
    echo "  $0 --list                       # 로컬 백업 목록"
    echo "  $0 --list-s3                    # S3 백업 목록"
    exit 0
}

list_local_backups() {
    log "로컬 백업 목록 (${BACKUP_DIR}):"
    echo ""
    if [[ -d "${BACKUP_DIR}" ]]; then
        ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | awk '{print "  " $NF " (" $5 ")"}'  || echo "  (백업 없음)"
    else
        echo "  (백업 디렉토리 없음)"
    fi
    exit 0
}

list_s3_backups() {
    if ! command -v aws &> /dev/null; then
        error_exit "AWS CLI가 설치되지 않았습니다."
    fi

    log "S3 백업 목록 (s3://${S3_BUCKET}/):"
    echo ""
    aws s3 ls "s3://${S3_BUCKET}/" | awk '{print "  " $4 " (" $3 " bytes, " $1 " " $2 ")"}'
    exit 0
}

# ============================================
# 인자 파싱
# ============================================
BACKUP_FILE=""
FROM_S3=false

case "${1:-}" in
    --help|-h)
        show_usage
        ;;
    --list)
        list_local_backups
        ;;
    --list-s3)
        list_s3_backups
        ;;
    --from-s3)
        FROM_S3=true
        BACKUP_FILE="${2:-}"
        if [[ -z "${BACKUP_FILE}" ]]; then
            error_exit "--from-s3 옵션에는 파일명이 필요합니다."
        fi
        ;;
    "")
        # 최신 로컬 백업 찾기
        BACKUP_FILE=$(ls -t "${BACKUP_DIR}"/codemate_*.sql.gz 2>/dev/null | head -n1 || true)
        if [[ -z "${BACKUP_FILE}" ]]; then
            error_exit "로컬 백업 파일이 없습니다. ${BACKUP_DIR} 확인 필요"
        fi
        ;;
    *)
        BACKUP_FILE="$1"
        ;;
esac

# ============================================
# 사전 체크
# ============================================
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    error_exit "MySQL 컨테이너 '${CONTAINER_NAME}'가 실행중이 아닙니다."
fi

# ============================================
# S3에서 다운로드 (필요시)
# ============================================
if [[ "${FROM_S3}" == true ]]; then
    if ! command -v aws &> /dev/null; then
        error_exit "AWS CLI가 설치되지 않았습니다."
    fi

    log "S3에서 다운로드 중: ${BACKUP_FILE}"
    TEMP_FILE="/tmp/${BACKUP_FILE}"
    aws s3 cp "s3://${S3_BUCKET}/${BACKUP_FILE}" "${TEMP_FILE}"
    BACKUP_FILE="${TEMP_FILE}"
fi

# 파일 존재 확인
if [[ ! -f "${BACKUP_FILE}" ]]; then
    error_exit "백업 파일을 찾을 수 없습니다: ${BACKUP_FILE}"
fi

# ============================================
# 복원 확인
# ============================================
log "복원할 백업 파일: ${BACKUP_FILE}"
log "대상 데이터베이스: ${DB_NAME}"
echo ""
echo "!!! 경고: 기존 데이터가 모두 삭제됩니다 !!!"
echo ""
read -p "계속하시겠습니까? (yes를 입력): " CONFIRM

if [[ "${CONFIRM}" != "yes" ]]; then
    log "복원 취소됨"
    exit 0
fi

# ============================================
# 복원 실행
# ============================================
log "데이터베이스 복원 중..."

gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" mysql \
    -u"${DB_USER}" \
    -p"${DB_PASSWORD}" \
    "${DB_NAME}"

log "복원 완료!"

# S3에서 다운받은 임시 파일 정리
if [[ "${FROM_S3}" == true ]] && [[ -f "/tmp/${BACKUP_FILE##*/}" ]]; then
    rm -f "/tmp/${BACKUP_FILE##*/}"
fi

exit 0