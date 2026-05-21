package com.github.suzumiyaaoba.thymeleaf.snapshot.sample;

import com.github.suzumiyaaoba.thymeleaf.snapshot.Snapshot;
import com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotTest;
import com.github.suzumiyaaoba.thymeleaf.snapshot.ThymeleafSnapshotExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Demonstrates inline templates: the template string is embedded directly in the annotation, useful
 * for small fragments or email bodies.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
class WelcomeEmailTest {

  private static final String TEMPLATE =
      """
            <div class="email">
              <h2 th:text="|Welcome, ${name}!|">Welcome!</h2>
              <p>Your account has been created successfully.</p>
              <p>Click the link below to activate your account:</p>
              <a th:href="${activationUrl}" class="cta">Activate Account</a>
              <p class="footer">
                If you did not create this account, you can safely ignore this email.
              </p>
            </div>
            """;

  @SnapshotTest(inlineTemplate = TEMPLATE)
  void shouldRenderWelcomeEmail(Snapshot snapshot) {
    snapshot
        .setVariable("name", "Alice")
        .setVariable("activationUrl", "https://example.com/activate/abc123")
        .assertMatchesSnapshot();
  }

  @SnapshotTest(inlineTemplate = TEMPLATE)
  void shouldRenderWelcomeEmailForDifferentUser(Snapshot snapshot) {
    snapshot
        .setVariable("name", "Bob")
        .setVariable("activationUrl", "https://example.com/activate/xyz789")
        .assertMatchesSnapshot();
  }
}
