package com.mostafa.eticket.controller;

import com.jayway.jsonpath.JsonPath;
import com.mostafa.eticket.service.InvitationEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationEmailService invitationEmailService;

    private static String agentJson(String username, String orgName) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + username + "@b.com\","
                + "\"password\":\"secret123\",\"organizationName\":\"" + orgName + "\"}";
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String registerAgent(String username, String orgName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson(username, orgName)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String registerViewerSelf(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/viewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\","
                                + "\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String inviteViewer(String agentToken, String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/invitations")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
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
    void registerAgentRejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" \",\"email\":\"e@b.com\",\"password\":\"secret123\",\"organizationName\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerViewerSelfReturnsCreatedWithToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/viewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer1\",\"password\":\"secret123\",\"email\":\"v1@b.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void registerViewerSelfReturns409OnDuplicateUsername() throws Exception {
        registerViewerSelf("viewer-dup", "v@b.com");
        mockMvc.perform(post("/api/v1/auth/register/viewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer-dup\",\"password\":\"secret123\",\"email\":\"other@b.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken: viewer-dup"));
    }

    @Test
    void acceptInvitationIsPublicAndRejectsUnknownToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"deadbeef\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invitation not found"));
    }

    @Test
    void acceptInvitationReturnsFreshTokenWithoutAuthentication() throws Exception {
        String agent = registerAgent("acme-agent1", "AcmeAuth1");
        String pendingViewer = registerViewerSelf("viewer-accept", "vac@b.com");
        String invite = inviteViewer(agent, "viewer-accept");

        mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + invite + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + invite + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invitation has already been used"));

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + pendingViewer))
                .andExpect(status().isOk());
    }

    @Test
    void createInvitationRejectsMissingViewerAccount() throws Exception {
        String agent = registerAgent("acme-agent3", "AcmeAuth3");

        mockMvc.perform(post("/api/v1/auth/invitations")
                        .header("Authorization", "Bearer " + agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(emptyOrNullString())));
    }
}