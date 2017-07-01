package crabzilla.vertx.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static crabzilla.vertx.DbHelper.initDb;
import static crabzilla.vertx.DbHelper.sqlLines;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(VertxUnitRunner.class)
public class VertxEventRepositoryTest {

  @Rule
  public MySQLContainer mysql = new MySQLContainer();

  Vertx vertx;
  VertxEventRepository repo;

  @Before
  public void setup(TestContext context) throws IOException, URISyntaxException {

    this.vertx =Vertx.vertx();

    val mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    HikariConfig config = new HikariConfig();
    config.setDriverClassName(mysql.getDriverClassName());
    config.setJdbcUrl(mysql.getJdbcUrl());
    config.setUsername(mysql.getUsername());
    config.setPassword(mysql.getPassword());
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    val jdbcClient = JDBCClient.create(vertx, new HikariDataSource(config));

    asList(sqlLines("/V1__write_model.sql")).forEach(sql -> {
      if (!sql.trim().isEmpty()) initDb(jdbcClient, sql);
    });

    this.repo = new VertxEventRepository(Customer.class, jdbcClient);
    
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void pingDb(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer");
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = UnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    repo.append(expectedUow, aLong -> {

      assertThat(aLong).isEqualTo(1);

      async.complete();

    });
    
  }

}
