package crabzilla.vertx;

import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DbHelper {

  public static void initDb(JDBCClient jdbcClient, String sql) {
    jdbcClient.getConnection(conn -> {
      if (conn.failed()) {
        throw new RuntimeException(conn.cause());
      }
      val sqlConn = conn.result();
      sqlConn.update(sql, updateResultAsyncResult -> {
        if (!updateResultAsyncResult.succeeded()) {
          throw new RuntimeException(updateResultAsyncResult.cause());
        }
      });
    });
  }

  public static String[] sqlLines(String fileName) throws URISyntaxException, IOException {
    val uri = DbHelper.class.getResource(fileName).toURI();
    val path = Paths.get(uri);
    String content = new String(Files.readAllBytes(path));
    return content.split(";");
  }

}
