package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.util.Locale;
import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Handles Thymeleaf template rendering for snapshot tests.
 *
 * <p>This class manages the configuration and lifecycle of the Thymeleaf {@link TemplateEngine},
 * supporting both classpath-based and inline templates.
 */
public final class ThymeleafRenderer {

  private final TemplateEngine classpathEngine;
  private final TemplateEngine inlineEngine;

  /**
   * Creates a new renderer with the specified configuration and template mode.
   *
   * @param prefix template prefix for classpath resolution
   * @param suffix template suffix for classpath resolution
   * @param characterEncoding character encoding for template processing
   * @param templateMode Thymeleaf template mode
   */
  public ThymeleafRenderer(
      String prefix, String suffix, String characterEncoding, TemplateMode templateMode) {
    this.classpathEngine = createClasspathEngine(prefix, suffix, characterEncoding, templateMode);
    this.inlineEngine = createInlineEngine(templateMode);
  }

  /**
   * Creates a new renderer defaulting to {@link TemplateMode#HTML}.
   *
   * @param prefix template prefix for classpath resolution
   * @param suffix template suffix for classpath resolution
   * @param characterEncoding character encoding for template processing
   */
  public ThymeleafRenderer(String prefix, String suffix, String characterEncoding) {
    this(prefix, suffix, characterEncoding, TemplateMode.HTML);
  }

  /**
   * Renders a template from the classpath.
   *
   * @param templateName the template name (without prefix/suffix)
   * @param variables the template variables
   * @param locale the locale for rendering
   * @return the rendered string
   */
  public String render(String templateName, Map<String, Object> variables, Locale locale) {
    Context context = new Context(locale);
    context.setVariables(variables);
    return classpathEngine.process(templateName, context);
  }

  /**
   * Renders an inline template string.
   *
   * @param templateContent the template string
   * @param variables the template variables
   * @param locale the locale for rendering
   * @return the rendered string
   */
  public String renderInline(String templateContent, Map<String, Object> variables, Locale locale) {
    Context context = new Context(locale);
    context.setVariables(variables);
    return inlineEngine.process(templateContent, context);
  }

  private static TemplateEngine createClasspathEngine(
      String prefix, String suffix, String characterEncoding, TemplateMode templateMode) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix(prefix);
    resolver.setSuffix(suffix);
    resolver.setTemplateMode(templateMode);
    resolver.setCharacterEncoding(characterEncoding);
    resolver.setCacheable(false); // Disable caching for tests

    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  private static TemplateEngine createInlineEngine(TemplateMode templateMode) {
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(templateMode);
    resolver.setCacheable(false);

    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }
}
