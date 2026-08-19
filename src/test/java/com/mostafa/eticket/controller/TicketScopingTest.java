package com.mostafa.eticket.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketScopingTest {

    @Autowired
    private MockMvc mockMvc;

    private static String agentJson(String username, String orgName) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + username + "@b.com\","
                + "\"password\":\"secret123\",\"organizationName\":\"" + orgName + "\"}";
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String ticketJson() {
        return "{\"title\":\"Outage\",\"description\":\"Systems down\","
                + "\"requesterEmail\":\"reporter@b.com\",\"priority\":\"HIGH\"}";
    }

    private String registerAgent(String username, String orgName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentJson(username, orgName)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long createTicket(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String inviteViewer(String agentToken, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/invitations")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String registerViewer(String token, String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/viewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"username\":\"" + username
                                + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void agentCreatesTicketScopedToOwnOrganization() throws Exception {
        String token = registerAgent("scope-alice1", "ScopeAcme1");

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.organizationId", not(nullValue())));
    }

    @Test
    void agentsSeeOnlyTheirOwnTickets() throws Exception {
        String alice = registerAgent("scope-alice2", "ScopeAcme2");
        String bob = registerAgent("scope-bob2", "ScopeBob2");

        createTicket(alice, ticketJson());

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void crossOrganizationReadAndWriteReturns404() throws Exception {
        String alice = registerAgent("scope-alice3", "ScopeAcme3");
        String bob = registerAgent("scope-bob3", "ScopeBob3");

        long ticketId = createTicket(alice, ticketJson());

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijack\",\"description\":\"nope\",\"priority\":\"LOW\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewerCanReadOwnOrganizationButCannotWrite() throws Exception {
        String alice = registerAgent("scope-alice4", "ScopeAcme4");
        long ticketId = createTicket(alice, ticketJson());
        String invite = inviteViewer(alice, "viewer4@b.com");
        String viewer = registerViewer(invite, "scope-viewer4");

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesAllAndCanCreateForSpecifiedOrganization() throws Exception {
        String alice = registerAgent("scope-alice5", "ScopeAcme5");
        String bob = registerAgent("scope-bob5", "ScopeBob5");

        long aliceTicket = createTicket(alice, ticketJson());
        createTicket(bob, ticketJson());
        MvcResult created = mockMvc.perform(get("/api/v1/tickets/" + aliceTicket)
                        .header("Authorization", "Bearer " + alice))
                .andReturn();
        long aliceOrg = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.organizationId"))
                .longValue();

        String admin = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/tickets/" + aliceTicket)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Admin ticket\",\"description\":\"For acme\","
                                + "\"requesterEmail\":\"r@b.com\",\"priority\":\"LOW\","
                                + "\"organizationId\":" + aliceOrg + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(aliceOrg));
    }

    @Test
    void adminCreateWithoutOrganizationIdReturns400() throws Exception {
        String admin = login("admin", "admin123");

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("organizationId is required")));
    }

    @Test
    void agentSendingOrganizationIdReturns400() throws Exception {
        String alice = registerAgent("scope-alice6", "ScopeAcme6");

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Outage\",\"description\":\"Systems down\","
                                + "\"requesterEmail\":\"r@b.com\",\"priority\":\"HIGH\","
                                + "\"organizationId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Only ADMIN")));
    }
}