package crabzilla.stack.vertx.codecs.fst;

import crabzilla.model.AggregateRootId;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.nustaq.serialization.FSTConfiguration;

import javax.inject.Inject;

public class AggregateRootIdCodec implements MessageCodec<AggregateRootId, AggregateRootId> {

  final FSTConfiguration fst;

  @Inject
  public AggregateRootIdCodec(FSTConfiguration fst) {
    this.fst = fst;
  }

  @Override
  public void encodeToWire(Buffer buffer, AggregateRootId aggregateRootId) {

    final byte barray[] = fst.asByteArray(aggregateRootId);

    // Write data into given buffer
    buffer.appendInt(barray.length);
    buffer.appendBytes(barray);
  }

  @Override
  public AggregateRootId decodeFromWire(int pos, Buffer buffer) {

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

    return (AggregateRootId) readObj;
  }

  @Override
  public AggregateRootId transform(AggregateRootId AggregateRootId) {
    return AggregateRootId;
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
