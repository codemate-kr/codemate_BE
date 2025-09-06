package com.ryu.studyhelper.auth.token;

public interface RefreshTokenService {
    
    /**
     * Refresh Token을 저장합니다.
     * @param id 사용자 ID
     * @param refreshToken Refresh Token
     */
    void saveRefreshToken(Long id, String refreshToken);
    
    /**
     * Refresh Token을 조회합니다.
     * @param id 사용자 ID
     * @return Refresh Token (없으면 null)
     */
    String getRefreshToken(Long id);
    
    /**
     * Refresh Token을 삭제합니다.
     * @param id 사용자 ID
     */
    void deleteRefreshToken(Long id);
    
    /**
     * Refresh Token의 존재 여부를 확인합니다.
     * @param id 사용자 ID
     * @return 존재 여부
     */
    boolean existsRefreshToken(Long id);
    
    /**
     * Refresh Token을 검증하고 새로운 Access Token을 발급합니다.
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token
     * @throws RefreshTokenException 토큰이 유효하지 않은 경우
     */
    String refreshAccessToken(String refreshToken) throws RefreshTokenException;
    
    /**
     * 저장된 Refresh Token과 요청으로 온 Refresh Token이 일치하는지 검증합니다.
     * @param id 사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @throws RefreshTokenException 토큰이 일치하지 않거나 유효하지 않은 경우
     */
    void validateRefreshToken(Long id, String refreshToken) throws RefreshTokenException;
}