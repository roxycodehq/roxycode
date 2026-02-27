package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheListControllerTest {

    @Test
    void testControllerIsAvailableInContext() {
        try (ApplicationContext context = ApplicationContext.run(java.util.Map.of("roxycode.preferences.path", "org/roxycode/gui/test"))) {
            CacheListController controller = context.getBean(CacheListController.class);
            assertNotNull(controller);
        }
    }
}
