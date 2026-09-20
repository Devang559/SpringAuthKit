package io.github.devang559.authkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.devang559.authkit.support.CapturingEmailService;
import io.github.devang559.authkit.support.TestEmailConfig;
import com.example.authkitdemo.AuthKitTestApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AuthKitTestApplication.class, TestEmailConfig.class})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
class AuthKitControllerTest {

    @Autowired MockMvc mvc;
    @Autowired CapturingEmailService emailCapture;
    @Autowired ObjectMapper om;

    private String accessToken(String responseBody) throws Exception {
        return om.readTree(responseBody).get("accessToken").asText();
    }

    private String refreshToken(String responseBody) throws Exception {
        return om.readTree(responseBody).get("refreshToken").asText();
    }

    private String register(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\","
                + "\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        return emailCapture.getLastOtp(email);
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"identifier\":\"" + email + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void fullFlow() throws Exception {
        String email = "flow_" + UUID.randomUUID() + "@example.com";
        String password = "Secret123";

        String otp = register(email, password);

        String loginBody = "{\"identifier\":\"" + email + "\",\"password\":\"" + password + "\"}";
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());

        String verify = "{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\",\"purpose\":\"EMAIL_VERIFICATION\"}";
        mvc.perform(post("/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isOk());

        String loginResponse = login(email, password);
        String accessToken = accessToken(loginResponse);
        String refreshToken = refreshToken(loginResponse);
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        mvc.perform(get("/api/test")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/test").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        String refreshResponse = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRefreshToken = refreshToken(refreshResponse);
        assertThat(newRefreshToken).isNotBlank();

        String logoutBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(logoutBody))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized());

        String newRefreshBody = "{\"refreshToken\":\"" + newRefreshToken + "\"}";
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(newRefreshBody))
                .andExpect(status().isOk());
    }

    @Test
    void loginWrongPassword() throws Exception {
        String email = "wrong_" + UUID.randomUUID() + "@example.com";
        String password = "Secret123";
        String otp = register(email, password);

        String verify = "{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\",\"purpose\":\"EMAIL_VERIFICATION\"}";
        mvc.perform(post("/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isOk());

        String loginBody = "{\"identifier\":\"" + email + "\",\"password\":\"DefinitelyWrong\"}";
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void registerDuplicateEmail() throws Exception {
        String email = "dup_" + UUID.randomUUID() + "@example.com";
        register(email, "Secret123");

        String body = "{\"email\":\"" + email + "\",\"password\":\"Secret123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWeakPassword() throws Exception {
        String body = "{\"email\":\"weak_" + UUID.randomUUID() + "@example.com\",\"password\":\"short\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutToken() throws Exception {
        mvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
    }
}
