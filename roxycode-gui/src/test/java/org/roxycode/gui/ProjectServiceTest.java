package org.roxycode.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;

class ProjectServiceTest {

    private Preferences prefs;

    private ProjectService projectService;

    private static final String TEST_PREF_PATH = "org/roxycode/gui/test/ProjectServiceTest";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws BackingStoreException {
        prefs = Preferences.userRoot().node(TEST_PREF_PATH);
        prefs.clear();
        projectService = new ProjectService(prefs);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        if (prefs != null) {
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
    }

    @Test
    void testLocalCacheInfoRefresh() throws IOException {
        projectService.setProjectPath(tempDir.toString());
        // Initially should be not found
        projectService.refreshLocalCacheInfo();
        Assertions.assertEquals("Not found", projectService.localCachePathProperty().get());
        // Create dummy cache file
        Path roxyDir = tempDir.resolve(ProjectService.ROXY_DIR).resolve(ProjectService.CACHE_DIR);
        Files.createDirectories(roxyDir);
        Path cacheFile = roxyDir.resolve(ProjectService.CACHE_FILE);
        String content = "dummy content";
        Files.writeString(cacheFile, content);
        projectService.refreshLocalCacheInfo();
        Assertions.assertEquals(cacheFile.toAbsolutePath().toString(), projectService.localCachePathProperty().get());
        Assertions.assertNotEquals("-", projectService.localCacheSizeProperty().get());
        Assertions.assertNotEquals("-", projectService.localCacheTimeProperty().get());
        Assertions.assertNotEquals("-", projectService.localCacheTokensProperty().get());
    }

    @Test
    void testProjectChangeClearsSession() {
        projectService.setProjectPath("/path/1");
        projectService.setCurrentCacheName("cache1");
        projectService.setProjectPath("/path/2");
        Assertions.assertNull(projectService.getCurrentCacheName());
    }
}
