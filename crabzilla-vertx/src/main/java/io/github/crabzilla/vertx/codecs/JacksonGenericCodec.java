package io.github.crabzilla.vertx.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import lombok.SneakyThrows;

import javax.inject.Inject;

public class JacksonGenericCodec<T> implements MessageCodec<T, T> {

  final ObjectMapper mapper;
  final Class<?> clazz;

  @Inject
  public JacksonGenericCodec(ObjectMapper mapper, Class<?> clazz) {
    this.mapper = mapper;
    this.clazz = clazz;
  }

  @Override
  @SneakyThrows
  public void encodeToWire(Buffer buffer, T obj) {

    final byte barray[] = mapper.writerFor(clazz).writeValueAsBytes(obj);

    // Write data into given buffer
    buffer.appendInt(barray.length);
    buffer.appendBytes(barray);
  }

  @Override
  public T decodeFromWire(int pos, Buffer buffer) {

    // My custom message starting from this *position* SUCCESS buffer
    int _pos = pos;

    // Length SUCCESS JSON
    int length = buffer.getInt(_pos);

    // Get JSON string by it`s length
    // Jump 4 because getInt() == 4 bytes
    final byte[] content = buffer.getBytes(_pos += 4, _pos + length);
    Object readObj;

    try {
      readObj = mapper.readValue(content, clazz);
    } catch (Exception e) {
      throw new RuntimeException("When decodingFromWire", e);
    }

    return (T) readObj;
  }

  @Override
  public T transform(T obj) {
    return obj;
  }

  @Override
  public String name() {
    // Each codec must have a unique name.
    // This is used to identify a codec when sending a message and for unregistering codecs.
    return clazz.getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }

}
