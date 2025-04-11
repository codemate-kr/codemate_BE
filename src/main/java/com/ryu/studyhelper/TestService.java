package com.ryu.studyhelper;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TestService {
    private final RestTemplate restTemplate;
    public TestService(){
        this.restTemplate = new RestTemplate();
    }

    public void getUnsolvedProblems(String handle) {
        String url = String.format(
                "https://solved.ac/api/v3/search/problem?query=!solved_by:%s&sort=level&direction=asc&size=10",
                handle);
//        ResponseEntity<SolvedResponse> response = restTemplate.getForEntity(url, SolvedResponse.class);


        System.out.println("Hello World");


    }
}
