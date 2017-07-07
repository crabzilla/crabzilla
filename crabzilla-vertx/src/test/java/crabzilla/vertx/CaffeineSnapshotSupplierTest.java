package crabzilla.vertx;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CaffeineSnapshotSupplierTest {

  LoadingCache<String, String> cache = Caffeine.newBuilder().build(key -> "result for key=" + key);

  @Test
  public void loadingCacheReallyLoad() {

    assertThat("result for key=1").isEqualTo(cache.get("1"));
    assertThat("result for key=2").isEqualTo(cache.get("2"));
    assertThat("result for key=1").isEqualTo(cache.getIfPresent("1"));

  }

}
