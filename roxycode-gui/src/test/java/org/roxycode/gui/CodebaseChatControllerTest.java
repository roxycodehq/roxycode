package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CodebaseChatControllerTest {
    @Test
    void testControllerIsAvailableInContext() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CodebaseChatController controller = context.getBean(CodebaseChatController.class);
            assertNotNull(controller);
        }
    }
}
