package io.github.crabzilla.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonGenericCodecTest {

  static final JacksonGenericCodec<SimplePojo> codec = new JacksonGenericCodec<>(new ObjectMapper(), SimplePojo.class);
  static final SimplePojo pojo =  new SimplePojo("Maria João", 21);

  @Test
  @DisplayName("it can encode")
  void encodeToWire() {
    Buffer buffer = new BufferImpl();
    codec.encodeToWire(buffer, pojo);
    assertThat(buffer.length()).isGreaterThan(0);
  }

  @Test
  @DisplayName("it can decode")
  void decode() {
    Buffer buffer = new BufferImpl();
    codec.encodeToWire(buffer, pojo);
    assertThat(buffer.length()).isGreaterThan(0);
    assertThat(pojo).isEqualTo(codec.decodeFromWire(0, buffer));
  }

  @Test
  @DisplayName("it can transform")
  void transform() {
    assertThat(pojo).isSameAs(codec.transform(pojo));
  }

  @Test
  @DisplayName("it has a name")
  void name() {
    assertThat("SimplePojo").isEqualTo(codec.name());
  }

  @Test
  void systemCodecID() {
    assertThat(-1).isEqualTo(codec.systemCodecID());
  }

  static class SimplePojo {
    String name;
    int age;
    SimplePojo() {}
    SimplePojo(String name, int age) {
      this.name = name;
      this.age = age;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public int getAge() {
      return age;
    }
    public void setAge(int age) {
      this.age = age;
    }
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SimplePojo that = (SimplePojo) o;
      return age == that.age &&
        Objects.equals(name, that.name);
    }
    @Override
    public int hashCode() {
      return Objects.hash(name, age);
    }
  }

}
