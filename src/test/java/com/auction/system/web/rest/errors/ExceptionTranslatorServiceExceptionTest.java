package com.auction.system.web.rest.errors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Checks that {@link com.auction.system.service.BadRequestServiceException} thrown by the service layer
 * is rendered exactly like a {@link BadRequestAlertException}. Runs without the Spring context or Docker.
 */
class ExceptionTranslatorServiceExceptionTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExceptionTranslator translator = new ExceptionTranslator(new MockEnvironment());
        ReflectionTestUtils.setField(translator, "applicationName", "auctionSystemApp");
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTranslatorTestController()).setControllerAdvice(translator).build();
    }

    @Test
    void serviceBadRequestIsTranslatedToBadRequestAlert() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/service-bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.testkey"))
            .andExpect(jsonPath("$.params").value("testEntity"))
            .andExpect(header().string("X-auctionSystemApp-error", "error.testkey"))
            .andExpect(header().string("X-auctionSystemApp-params", "testEntity"));
    }
}
