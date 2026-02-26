package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheListControllerTest {

    @Test
    void testControllerIsAvailableInContext() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CacheListController controller = context.getBean(CacheListController.class);
            assertNotNull(controller);
        }
    }
}
