package com.github.suzumiyaaoba.thymeleaf.snapshot.sample;

import com.github.suzumiyaaoba.thymeleaf.snapshot.Snapshot;
import com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotConfig;
import com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotTest;
import com.github.suzumiyaaoba.thymeleaf.snapshot.ThymeleafSnapshotExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Demonstrates named snapshots: a single test method asserts multiple
 * rendering states, each stored under a distinct snapshot name.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
@SnapshotConfig(prettyPrint = true)
class UserProfileTest {

    @SnapshotTest(template = "user-profile")
    void shouldRenderDifferentRoles(Snapshot snapshot) {
        snapshot.setVariable("user", new User("Alice", "Platform engineer", "admin"))
                .assertMatchesSnapshot("admin");

        snapshot.clearVariables()
                .setVariable("user", new User("Bob", "Open-source contributor", "member"))
                .assertMatchesSnapshot("member");
    }

    public static final class User {
        private final String name;
        private final String bio;
        private final String role;

        public User(String name, String bio, String role) {
            this.name = name;
            this.bio = bio;
            this.role = role;
        }

        public String getName() { return name; }
        public String getBio() { return bio; }
        public String getRole() { return role; }
    }
}
