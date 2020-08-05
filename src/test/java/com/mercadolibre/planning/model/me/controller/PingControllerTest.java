package com.mercadolibre.planning.model.me.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("development")
@WebMvcTest(controllers = PingController.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class PingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Requests to /ping should response a pong")
    public void testPingResponse() throws Exception {
        // WHEN
        final ResultActions result = mvc.perform(get("/ping"));

        // THEN
        result.andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }
}
