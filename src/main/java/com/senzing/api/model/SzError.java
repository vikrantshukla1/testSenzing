package com.senzing.api.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import com.senzing.g2.engine.G2Fallible;

/**
 * Describes an error that occurred.
 */
public class SzError {
  /**
   * The associated error code (if any)
   */
  private String code;

  /**
   * The associated error message.
   */
  private String message;

  /**
   * Default constructor.
   */
  public SzError() {
    this(null, null);
  }

  /**
   * Constructs with the specified error message.
   *
   * @param message The message to associate.
   */
  public SzError(String message) {
    this(null, message);
  }

  /**
   * Constructs with the specified error code and error message.
   *
   * @param code The error code to associate.
   *
   * @param message The message to associate.
   */
  public SzError(String code, String message) {
    this.code     = code;
    this.message  = message;
  }
  /**
   * Constructs with the specified error code and error message.
   *
   * @param t The {@link Throwable} that triggered the error.
   */
  public SzError(Throwable t) {
    this.code     = null;
    this.message  = formatThrowable(t);
    t.printStackTrace();
  }

  /**
   * Constructs with the last exception information from the specified
   * {@link G2Fallible}.
   *
   * @param f The {@link G2Fallible} to get the error information from.
   */
  public SzError(G2Fallible f) {
    int errorCode = f.getLastExceptionCode();
    this.code     = "" + errorCode;
    this.message  = f.getLastException();
    f.clearLastException();
  }

  /**
   * Formats a throwable into a message string.
   *
   * @param t The {@link Throwable} to format.
   */
  private static String formatThrowable(Throwable t)
  {
    if (t == null) return null;
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println(t.getMessage());
    pw.println();
    t.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }

  /**
   * Gets the associated error code (if any).
   *
   * @return The associated error code, or <tt>null</tt> if none associated.
   */
  public String getCode() {
    return this.code;
  }

  /**
   * Sets the associated error code.  Set to <tt>null</tt> if none.
   *
   * @param code The associated error code or <tt>null</tt> if none.
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Gets the associated message for the error.
   *
   * @return The associated error message.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Sets the associated message for the error.
   *
   * @param message The message to associate.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Produces a diagnostic {@link String} describing this instance.
   *
   * @return A diagnostic {@link String} describing this instance.
   */
  @Override
  public String toString() {
    return "SzError{" +
        "code='" + this.getCode() + '\'' +
        ", message='" + this.getMessage() + '\'' +
        '}';
  }
}