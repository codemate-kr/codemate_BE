package com.ryu.studyhelper.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessToken(String accessToken, long expiresIn) {
    @JsonCreator
    public AccessToken(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {

        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
