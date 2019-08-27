package io.github.crabzilla.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class JacksonGenericCodec<T> implements MessageCodec<T, T> {

  final ObjectMapper mapper;
  final Class<?> clazz;

  public JacksonGenericCodec(ObjectMapper mapper, Class<?> clazz) {
    this.mapper = mapper;
    this.clazz = clazz;
  }

  @Override
  public void encodeToWire(Buffer buffer, T obj) {

    final byte[] barray;
    try {
      barray = mapper.writerFor(clazz).writeValueAsBytes(obj);
      // Write data into given buffer
      buffer.appendInt(barray.length);
      buffer.appendBytes(barray);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Encoding Json", e);
    }


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
      throw new RuntimeException("Decoding Json", e);
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
