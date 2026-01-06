#!/bin/bash
# Blue-Green 무중단 배포 스크립트
# 사용법: ./deploy-blue-green.sh [이미지태그]
# 예시: ./deploy-blue-green.sh latest
#       ./deploy-blue-green.sh v1.2.3

set -e

# ==================== 설정 ====================
IMAGE_TAG="${1:-latest}"
DOCKER_USERNAME="${DOCKER_USERNAME:-ryuwillow}"
IMAGE_NAME="${DOCKER_USERNAME}/codemate:${IMAGE_TAG}"
NETWORK_NAME="ec2-user_codemate-network"
LOG_DIR="$HOME/logs"

# Nginx 설정 (심볼릭 링크 방식)
NGINX_CONF_DIR="/etc/nginx/conf.d"
NGINX_UPSTREAM_LINK="${NGINX_CONF_DIR}/upstream.conf"
NGINX_UPSTREAM_BLUE="${NGINX_CONF_DIR}/upstream-blue.upstream"
NGINX_UPSTREAM_GREEN="${NGINX_CONF_DIR}/upstream-green.upstream"

# Blue/Green 설정
BLUE_CONTAINER="codemate-blue"
GREEN_CONTAINER="codemate-green"
BLUE_PORT=8081
GREEN_PORT=8082
BLUE_MGMT_PORT=9091
GREEN_MGMT_PORT=9092

# ==================== 함수 ====================

# 현재 활성 컨테이너 확인 (심볼릭 링크로 판단)
get_active_container() {
    local current_link=$(readlink -f ${NGINX_UPSTREAM_LINK} 2>/dev/null)

    if [[ "$current_link" == *"upstream-blue.upstream" ]]; then
        echo "blue"
    elif [[ "$current_link" == *"upstream-green.upstream" ]]; then
        echo "green"
    else
        echo "none"
    fi
}

# 헬스체크 (포트를 인자로 받음)
wait_for_health() {
    local port=$1
    local max_attempts=40

    echo "   헬스체크 중 (포트: ${port})..."
    for i in $(seq 1 $max_attempts); do
        if curl -sf http://localhost:${port}/actuator/health > /dev/null 2>&1; then
            echo "   ✓ 헬스체크 통과!"
            return 0
        fi
        printf "."
        sleep 3
    done

    echo ""
    echo "   ✗ 헬스체크 실패 (2분 초과)"
    return 1
}

# 컨테이너 시작
start_container() {
    local name=$1
    local app_port=$2
    local mgmt_port=$3

    echo "   컨테이너 시작: ${name} (포트: ${app_port})"

    # 기존 컨테이너 정리
    docker stop ${name} 2>/dev/null || true
    docker rm ${name} 2>/dev/null || true

    # 새 컨테이너 실행
    docker run -d \
        --name ${name} \
        --restart unless-stopped \
        --network ${NETWORK_NAME} \
        -p ${app_port}:8080 \
        -p ${mgmt_port}:9090 \
        -v /etc/localtime:/etc/localtime:ro \
        -v /etc/timezone:/etc/timezone:ro \
        -v ${LOG_DIR}:/var/log/app \
        -e SPRING_PROFILES_ACTIVE=prod \
        --env-file ~/.env \
        ${IMAGE_NAME}
}

# Nginx upstream 전환 (심볼릭 링크 방식)
switch_nginx() {
    local target=$1  # "blue" 또는 "green"

    echo "   Nginx upstream 전환: ${target}"

    # 심볼릭 링크 변경
    if [ "$target" == "blue" ]; then
        sudo ln -sf ${NGINX_UPSTREAM_BLUE} ${NGINX_UPSTREAM_LINK}
    else
        sudo ln -sf ${NGINX_UPSTREAM_GREEN} ${NGINX_UPSTREAM_LINK}
    fi

    # Nginx 설정 테스트 및 리로드
    sudo nginx -t && sudo nginx -s reload
    echo "   ✓ Nginx 전환 완료!"
}

# 컨테이너 graceful shutdown
graceful_shutdown() {
    local name=$1

    if docker ps -q -f name=${name} > /dev/null 2>&1; then
        echo "   이전 컨테이너 종료: ${name}"
        docker stop ${name} --time 30 2>/dev/null || true
        docker rm ${name} 2>/dev/null || true
        echo "   ✓ 종료 완료!"
    fi
}

# ==================== 메인 로직 ====================

echo "=========================================="
echo "🚀 Blue-Green 무중단 배포 시작"
echo "   이미지: ${IMAGE_NAME}"
echo "=========================================="
echo ""

# 1. 로그 디렉토리 생성
mkdir -p ${LOG_DIR}

# 2. 최신 이미지 pull
echo "📦 [1/5] 이미지 다운로드"
docker pull ${IMAGE_NAME}
echo ""

# 3. 현재 활성 컨테이너 확인
echo "🔍 [2/5] 현재 상태 확인"
ACTIVE=$(get_active_container)
echo "   현재 활성: ${ACTIVE}"

if [ "$ACTIVE" == "blue" ]; then
    TARGET_NAME="green"
    TARGET_CONTAINER=$GREEN_CONTAINER
    TARGET_PORT=$GREEN_PORT
    TARGET_MGMT_PORT=$GREEN_MGMT_PORT
    OLD_CONTAINER=$BLUE_CONTAINER
elif [ "$ACTIVE" == "green" ]; then
    TARGET_NAME="blue"
    TARGET_CONTAINER=$BLUE_CONTAINER
    TARGET_PORT=$BLUE_PORT
    TARGET_MGMT_PORT=$BLUE_MGMT_PORT
    OLD_CONTAINER=$GREEN_CONTAINER
else
    # 초기 배포 (Blue로 시작)
    echo "   초기 배포 감지 - Blue로 시작"
    TARGET_NAME="blue"
    TARGET_CONTAINER=$BLUE_CONTAINER
    TARGET_PORT=$BLUE_PORT
    TARGET_MGMT_PORT=$BLUE_MGMT_PORT
    OLD_CONTAINER=""
fi

echo "   배포 대상: ${TARGET_CONTAINER} (포트: ${TARGET_PORT})"
echo ""

# 4. 새 컨테이너 시작
echo "🐳 [3/5] 새 컨테이너 배포"
start_container $TARGET_CONTAINER $TARGET_PORT $TARGET_MGMT_PORT
echo ""

# 5. 헬스체크
echo "⏳ [4/5] 헬스체크"
if ! wait_for_health $TARGET_MGMT_PORT; then
    echo ""
    echo "=========================================="
    echo "❌ 배포 실패: 헬스체크 타임아웃"
    echo "=========================================="
    echo ""
    echo "로그 확인:"
    docker logs ${TARGET_CONTAINER} --tail 30

    # 실패한 컨테이너 정리
    docker stop ${TARGET_CONTAINER} 2>/dev/null || true
    docker rm ${TARGET_CONTAINER} 2>/dev/null || true
    exit 1
fi
echo ""

# 6. Nginx 전환
echo "🔄 [5/5] 트래픽 전환"
switch_nginx $TARGET_NAME

# 7. 이전 컨테이너 종료
if [ -n "$OLD_CONTAINER" ]; then
    graceful_shutdown $OLD_CONTAINER
fi
echo ""

# 완료
echo "=========================================="
echo "✅ 무중단 배포 완료!"
echo "=========================================="
echo ""
echo "📋 현재 상태:"
echo "   활성 컨테이너: ${TARGET_CONTAINER}"
echo "   앱 포트: ${TARGET_PORT}"
echo "   관리 포트: ${TARGET_MGMT_PORT}"
echo ""
echo "📋 유용한 명령어:"
echo "   로그 보기:     docker logs -f ${TARGET_CONTAINER}"
echo "   로그 파일:     tail -f ${LOG_DIR}/application.log"
echo "   컨테이너 상태: docker ps"
echo "   롤백:         ./deploy-blue-green.sh  # 다시 실행하면 이전 버전으로"
echo ""