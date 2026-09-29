package br.com.diegobraun.ratelimiter.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"rate-limit.limit=2", "rate-limit.period=1h"})
@AutoConfigureMockMvc
class RateLimitApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returns429WithRateLimitHeadersWhenLimitIsExceeded() throws Exception {
        mockMvc.perform(get("/api/limited/token-bucket").header("X-Client-Id", "api-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "1"));
        mockMvc.perform(get("/api/limited/token-bucket").header("X-Client-Id", "api-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
        mockMvc.perform(get("/api/limited/token-bucket").header("X-Client-Id", "api-test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.algorithm").value("token-bucket"));
    }

    @Test
    void limitsAreIndependentPerClient() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/limited/fixed-window").header("X-Client-Id", "client-a"));
        }
        mockMvc.perform(get("/api/limited/fixed-window").header("X-Client-Id", "client-a"))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get("/api/limited/fixed-window").header("X-Client-Id", "client-b"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnknownAlgorithm() throws Exception {
        mockMvc.perform(get("/api/limited/does-not-exist"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulatesAllAlgorithms() throws Exception {
        mockMvc.perform(get("/api/simulations").param("pattern", "burst"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void validatesSimulationParameters() throws Exception {
        mockMvc.perform(get("/api/simulations").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }
}
