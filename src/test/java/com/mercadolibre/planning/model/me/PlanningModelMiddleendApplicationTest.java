package com.mercadolibre.planning.model.me;

import com.mercadolibre.planning.model.me.controller.PingController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("functional")
public class PlanningModelMiddleendApplicationTest {

    @Autowired
    private PingController pingController;

    @Test
    @DisplayName("Context Loads")
    public void contextLoads() {
        assertNotNull(pingController);
    }
}
