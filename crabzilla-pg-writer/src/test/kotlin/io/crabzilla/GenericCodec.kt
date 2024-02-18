package io.crabzilla

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutput
import java.io.ObjectOutputStream

class GenericCodec<T>(private val cls: Class<T>) : MessageCodec<T, T?> {
  override fun encodeToWire(
    buffer: Buffer,
    s: T,
  ) {
    val bos = ByteArrayOutputStream()
    var out: ObjectOutput? = null
    try {
      out = ObjectOutputStream(bos)
      out.writeObject(s)
      out.flush()
      val yourBytes = bos.toByteArray()
      buffer.appendInt(yourBytes.size)
      buffer.appendBytes(yourBytes)
      out.close()
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      try {
        bos.close()
      } catch (ex: IOException) {
        ex.printStackTrace()
      }
    }
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): T? {
    // My custom message starting from this *position* of buffer
    var position = pos

    // Length of JSON
    val length = buffer.getInt(position)

    // Jump 4 because getInt() == 4 bytes
    val yourBytes =
      buffer.getBytes(
        4.let {
          position += it
          position
        },
        length.let {
          position += it
          position
        },
      )
    val bis = ByteArrayInputStream(yourBytes)
    try {
      val ois = ObjectInputStream(bis)
      val msg = ois.readObject() as T
      ois.close()
      return msg
    } catch (e: IOException) {
      println("Listen failed " + e.message)
    } catch (e: ClassNotFoundException) {
      println("Listen failed " + e.message)
    } finally {
      try {
        bis.close()
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
    return null
  }

  override fun transform(customMessage: T): T? {
    // If a message is sent *locally* across the event bus.
    // This example sends message just as is
    return customMessage
  }

  override fun name(): String {
    // Each codec must have a unique name.
    // This is used to identify a codec when sending a message and for unregistering
    // codecs.
    return cls.getSimpleName() + "Codec"
  }

  override fun systemCodecID(): Byte {
    // Always -1
    return -1
  }
}
