package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
class CacheDetailsControllerTest {

    @Inject
    CacheDetailsController controller;

    @Test
    void testControllerIsAvailableInContext() {
        assertNotNull(controller);
    }
}