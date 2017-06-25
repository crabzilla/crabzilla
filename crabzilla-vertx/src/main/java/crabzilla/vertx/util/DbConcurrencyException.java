package crabzilla.vertx.util;

public class DbConcurrencyException extends RuntimeException {

  public DbConcurrencyException(String s) {
    super(s);
  }

}
