package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks snapshot access and successful test completion for a single test class. */
final class SnapshotTestRun {

  private final Set<String> expectedMethods;
  private final Set<String> completedMethods = ConcurrentHashMap.newKeySet();
  private final Set<Path> accessedPaths = ConcurrentHashMap.newKeySet();

  SnapshotTestRun(Class<?> testClass) {
    expectedMethods = snapshotTestMethods(testClass);
  }

  Set<Path> accessedPaths() {
    return accessedPaths;
  }

  void recordCompletion(Method method) {
    completedMethods.add(method.getName());
  }

  boolean isComplete() {
    return completedMethods.containsAll(expectedMethods);
  }

  /** Includes inherited tests; nested classes have their own run state. */
  private static Set<String> snapshotTestMethods(Class<?> testClass) {
    Set<String> methodNames = new HashSet<>();
    for (Class<?> cls = testClass; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
      for (Method method : cls.getDeclaredMethods()) {
        if (method.getAnnotation(SnapshotTest.class) != null) {
          methodNames.add(method.getName());
        }
      }
    }
    return Set.copyOf(methodNames);
  }
}
