package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import static org.junit.jupiter.api.Assertions.*;

class CacheListControllerTest {

    @Test
    void testControllerIsAvailableInContext() throws BackingStoreException {
        try (ApplicationContext context = ApplicationContext.run(java.util.Map.of("roxycode.preferences.path", "org/roxycode/gui/test/CacheListControllerTest"))) {
            CacheListController controller = context.getBean(CacheListController.class);
            assertNotNull(controller);
            
            // Cleanup
            Preferences prefs = context.getBean(Preferences.class);
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
    }
}
