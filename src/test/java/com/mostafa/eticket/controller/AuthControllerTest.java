package com.mostafa.eticket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String agentJson(String username, String orgName) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + username + "@b.com\","
                + "\"password\":\"secret123\",\"organizationName\":\"" + orgName + "\"}";
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void registerAgentReturnsCreatedWithToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson("alice1", "Acme")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void registerAgentReturns409OnDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson("alice2", "Acme2")));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson("alice2", "OtherOrg")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken: alice2"));
    }

    @Test
    void registerAgentReturns409OnDuplicateOrganizationName() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson("bob1", "SharedOrg")));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson("bob2", "SharedOrg")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Organization name already taken: SharedOrg"));
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(agentJson("carol1", "CarolCorp")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("carol1", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void loginReturns401ForWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(agentJson("dave1", "DaveCorp")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("dave1", "wrongpass")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerViewerRejectsUnknownToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/viewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"deadbeef\",\"username\":\"viewer1\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invitation not found"));
    }

    @Test
    void registerAgentRejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" \",\"email\":\"e@b.com\",\"password\":\"secret123\",\"organizationName\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }
}