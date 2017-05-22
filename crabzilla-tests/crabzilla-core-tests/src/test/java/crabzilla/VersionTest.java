package crabzilla;

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
  public void can_be_instantiated() {
    new Version(1);
  }

  @Test
  void a_non_positive_version_exception() {
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
    void next_version_is_based_on_next_long() {
      assertThat(version.nextVersion()).isEqualTo(new Version(2L));
    }

  }

}