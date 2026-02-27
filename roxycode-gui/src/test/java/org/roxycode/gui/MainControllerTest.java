package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainControllerTest {

    @Test
    void testControllerIsAvailableInContext() {
        try (ApplicationContext context = ApplicationContext.run(java.util.Map.of("roxycode.preferences.path", "org/roxycode/gui/test"))) {
            MainController controller = context.getBean(MainController.class);
            assertNotNull(controller);
        }
    }
}