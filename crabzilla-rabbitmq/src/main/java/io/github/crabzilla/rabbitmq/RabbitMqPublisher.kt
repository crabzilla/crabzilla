package io.github.crabzilla.rabbitmq

import io.github.crabzilla.core.EventBusChannels
import io.github.crabzilla.core.UnitOfWorkPublisher
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQOptions
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

class RabbitMqPublisher(private val client: RabbitMQClient) : UnitOfWorkPublisher {

  companion object {
    private val log = LoggerFactory.getLogger(RabbitMqPublisher::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
      val config = RabbitMQOptions()
      config.user = "guest"
      config.password = "guest"
      config.host = "localhost"
      config.port = 5672
//      config.virtualHost = "vhost1"
      val client = RabbitMQClient.create(Vertx.vertx(), config)
      client.start { gotConnected ->
        if (gotConnected.failed()) {
          log.error("Failed to connect")
          return@start
        }
        println(client.isConnected)
        client.exchangeDeclare(EventBusChannels.unitOfWorkChannel, "fanout", true, false) { createExchange ->
          if (createExchange.failed()) {
            log.error("Failed to create exchange")
            return@exchangeDeclare
          }
          val queueConfig = JsonObject().put("Single active consumer c", "true")
          client.queueDeclare("customer-summary", true, false, false, queueConfig) { createQueue ->
            if (createQueue.failed()) {
              log.error("Failed to create queue")
              return@queueDeclare
            }
            client.queueBind("customer-summary", EventBusChannels.unitOfWorkChannel, "") { bindQueue ->
              if (bindQueue.failed()) {
                log.error("Failed to bind queue")
                return@queueBind
              }
              // Create a stream of messages from a queue
              client.basicConsumer("customer-summary") { rabbitMQConsumerAsyncResult ->
                if (rabbitMQConsumerAsyncResult.succeeded()) {
                  println("RabbitMQ consumer created !")
                  val mqConsumer = rabbitMQConsumerAsyncResult.result()
                  mqConsumer.handler { message ->
                    println("Got message: ${message.body().toJson()}")
                  }
                } else {
                  rabbitMQConsumerAsyncResult.cause().printStackTrace()
                }
              }
//              client.basicGet("customer-summary", true) { getResult ->
//                if (getResult.succeeded()) {
//                  val msg: JsonObject = getResult.result()
//                  println("Got message: ${msg}")
//                } else {
//                  getResult.cause().printStackTrace()
//                }
//              }
//              client.basicConsumer("customer-summary", queueOptionsOf(autoAck = false)) { consumeResult ->
//                if (consumeResult.succeeded()) {
//                  println("RabbitMQ consumer created !")
//                  val consumer = consumeResult.result()
//                  // Set the handler which messages will be sent to
//                  consumer.handler { msg ->
//                    val json = JsonObject(msg.body())
//                    println("Got message: ${json.getString("body")}")
//                    // ack
//                    client.basicAck(json.getLong("deliveryTag"), false) { asyncResult ->
//
//                    }
//                  }
//                } else {
//                  consumeResult.cause().printStackTrace()
//                }
//              }
              val json = JsonObject().put("hello", "Hello RabbitMQ, from Vert.x 222 !")
                .put("time", LocalDateTime.now().toString())
              val message: JsonObject = json {
                obj("body" to json.encodePrettily())
              }
              client.basicPublish(EventBusChannels.unitOfWorkChannel, "", message) { pubResult ->
                if (pubResult.succeeded()) {
                  println("Message published !")
                } else {
                  pubResult.cause().printStackTrace()
                }
              }
            }
          }
        }
      }
    }
  }

  override fun publish(events: JsonObject) {
    if (log.isDebugEnabled) log.debug("will publish $events")
    // Put the channel in confirm mode. This can be done once at init.
    client.confirmSelect { confirmResult ->
      if (confirmResult.succeeded()) {
        client.basicPublish(EventBusChannels.unitOfWorkChannel, "", events) { pubResult ->
          if (pubResult.succeeded()) {
            // Check the message got confirmed by the broker.
            client.waitForConfirms { waitResult ->
              if (waitResult.succeeded()) {
                println("Message published !") } else {
                waitResult.cause().printStackTrace() }
            }
          } else {
            pubResult.cause().printStackTrace()
          }
        }
      } else {
        confirmResult.cause().printStackTrace()
      }
    }
  }
}
