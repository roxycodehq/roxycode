package org.roxycode.gui;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.prefs.Preferences;

/**
 * Factory for providing java.util.prefs.Preferences.
 */
@Factory
public class PreferencesFactory {

    @Value("${roxycode.preferences.path:org/roxycode/gui}")
    private String preferencePath;

    /**
     * Provides the Preferences bean.
     * @return the Preferences instance for the configured path
     */
    @Singleton
    public Preferences preferences() {
        return Preferences.userRoot().node(preferencePath);
    }
}
