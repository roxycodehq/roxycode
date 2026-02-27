package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.roxycode.gui.events.ChatStatusEvent;
import javafx.application.Platform;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class CodebaseChatControllerTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    @Test
    void testControllerIsSingleton() throws BackingStoreException {
        try (ApplicationContext context = ApplicationContext.run(java.util.Map.of("roxycode.preferences.path", "org/roxycode/gui/test/CodebaseChatControllerTest"))) {
            CodebaseChatController c1 = context.getBean(CodebaseChatController.class);
            CodebaseChatController c2 = context.getBean(CodebaseChatController.class);
            
            assertNotNull(c1);
            assertSame(c1, c2, "Controller should be a Singleton");
            
            // Cleanup
            Preferences prefs = context.getBean(Preferences.class);
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
    }

    @Test
    void testOnChatStatusDoesNotThrowNPEWhenDetached() throws BackingStoreException, InterruptedException {
        try (ApplicationContext context = ApplicationContext.run(java.util.Map.of("roxycode.preferences.path", "org/roxycode/gui/test/CodebaseChatControllerTest"))) {
            CodebaseChatController controller = context.getBean(CodebaseChatController.class);
            
            // We need to wait for Platform.runLater to execute or at least check it doesn't crash the calling thread
            CountDownLatch latch = new CountDownLatch(1);
            
            assertDoesNotThrow(() -> {
                controller.onChatStatus(new ChatStatusEvent("Testing status", true));
                // Add a small task to the queue to ensure previous runLaters are processed
                Platform.runLater(latch::countDown);
            });
            
            latch.await(2, TimeUnit.SECONDS);
            
            // Cleanup
            Preferences prefs = context.getBean(Preferences.class);
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
    }

    @Test
    void testEscapeJs() {
        assertEquals("Don\\'t Show", CodebaseChatController.escapeJs("Don't Show"));
        assertEquals("No quotes", CodebaseChatController.escapeJs("No quotes"));
        assertEquals("\\'\\'\\'", CodebaseChatController.escapeJs("'''"));
    }
}
