package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
@io.micronaut.context.annotation.Property(name = "roxycode.preferences.path", value = "org/roxycode/gui/test/CacheDetailsControllerTest")
class CacheDetailsControllerTest {

    @Inject
    CacheDetailsController controller;

    @Inject
    java.util.prefs.Preferences prefs;

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws java.util.prefs.BackingStoreException {
        if (prefs != null && prefs.nodeExists("")) {
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
    }

    @Test
    void testControllerIsAvailableInContext() {
        assertNotNull(controller);
    }
}