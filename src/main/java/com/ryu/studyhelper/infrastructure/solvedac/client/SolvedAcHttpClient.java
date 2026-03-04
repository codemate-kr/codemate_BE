package com.ryu.studyhelper.infrastructure.solvedac.client;

import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;

public interface SolvedAcHttpClient {

    SolvedAcUserResponse getUserInfo(String handle);

    ProblemSearchResponse searchProblems(String query, String sort, String direction);

    SolvedAcUserBioResponse getUserBio(String handle);
}