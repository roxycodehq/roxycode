package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainControllerTest {

    @Test
    void testControllerIsAvailableInContext() {
        try (ApplicationContext context = ApplicationContext.run()) {
            MainController controller = context.getBean(MainController.class);
            assertNotNull(controller);
        }
    }
}