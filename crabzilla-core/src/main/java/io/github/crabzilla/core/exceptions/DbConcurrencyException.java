package io.github.crabzilla.core.exceptions;

public class DbConcurrencyException extends RuntimeException {

  public DbConcurrencyException(String s) {
    super(s);
  }

}
