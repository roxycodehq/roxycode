package org.roxycode.gui;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Service to manage PebbleEngine and template rendering.
 */
@Singleton
public class TemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);
    private final PebbleEngine engine;

    public TemplateService() {
        this.engine = new PebbleEngine.Builder()
                .cacheActive(true)
                .build();
    }

    /**
     * Renders a template with the given context.
     * @param templatePath Path to the template relative to resources.
     * @param context Map of variables for the template.
     * @return Rendered string.
     */
    public String render(String templatePath, Map<String, Object> context) {
        try {
            PebbleTemplate compiledTemplate = engine.getTemplate(templatePath);
            Writer writer = new StringWriter();
            compiledTemplate.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            LOG.error("Failed to render template: " + templatePath, e);
            return "<html><body>Error rendering template: " + e.getMessage() + "</body></html>";
        }
    }
}
