package crabzilla.stack.vertx.codecs.fst;

import crabzilla.model.Event;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.nustaq.serialization.FSTConfiguration;

import javax.inject.Inject;

public class EventCodec implements MessageCodec<Event, Event> {

  final FSTConfiguration fst;

  @Inject
  public EventCodec(FSTConfiguration fst) {
    this.fst = fst;
  }

  @Override
  public void encodeToWire(Buffer buffer, Event Event) {

    final byte barray[] = fst.asByteArray(Event);

    // Write data into given buffer
    buffer.appendInt(barray.length);
    buffer.appendBytes(barray);
  }

  @Override
  public Event decodeFromWire(int pos, Buffer buffer) {

    // My custom message starting from this *position* of buffer
    int _pos = pos;

    // Length of JSON
    int length = buffer.getInt(_pos);

    // Get JSON string by it`s length
    // Jump 4 because getInt() == 4 bytes
    final byte[] content = buffer.getBytes(_pos += 4, _pos += length);
    Object readObj;

    try {
      readObj = fst.getObjectInput(content).readObject();
    } catch (Exception e) {
      throw new RuntimeException("When decodingFromWire", e);
    }

    return (Event) readObj;
  }

  @Override
  public Event transform(Event Event) {
    return Event;
  }

  @Override
  public String name() {
    // Each codec must have a unique name.
    // This is used to identify a codec when sending a message and for unregistering codecs.
    return this.getClass().getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }

}
