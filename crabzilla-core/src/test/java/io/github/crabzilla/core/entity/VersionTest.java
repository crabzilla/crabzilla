package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A Version")
public class VersionTest {

  Version version;

  @Test
  @DisplayName("can be instantiated")
  public void canBeInstantiated() {
    new Version(1);
  }

  @Test
  @DisplayName("a non positive version_exception")
  void aNonPositiveVersionException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Version(-3);
    });
  }

  @Nested
  @DisplayName("when new")
  public class WhenIsNew {

    @BeforeEach
    void version1() {
      version = new Version(1L);
    }

    @Test
    @DisplayName("next version is based on next long")
    void nextVersionIsBasedOnNextLong() {
      assertThat(version.nextVersion()).isEqualTo(new Version(2L));
    }

  }

}
