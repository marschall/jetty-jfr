package com.github.marschall.jetty.jfr;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Relational;
import jdk.jfr.StackTrace;

/**
 * An {@link Handler} that generates <a href="https://openjdk.java.net/jeps/328">Flight Recorder</a> events.
 */
public class JfrHandler extends HandlerWrapper {

  static final String EXCHANGE_ID_ATTRIBUTE = "com.github.marschall.jetty.jfr.exchangeId";

  private static final AtomicLong EXCHANGE_ID_GENERATOR = new AtomicLong();

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Long exchangeId = (Long) request.getAttribute(EXCHANGE_ID_ATTRIBUTE);
    if (exchangeId != null) {
      // dispatched request
      this.handleRelatedRequest(exchangeId, target, baseRequest, request, response);
    } else {
      this.handleNewRequest(target, baseRequest, request, response);
    }
  }


  private void handleNewRequest(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException {

    long newExchangeId = generateExchangeId();
    request.setAttribute(EXCHANGE_ID_ATTRIBUTE, newExchangeId);

    HttpEvent event = new HttpEvent();
    event.setExchangeId(newExchangeId);
    copyHttpRequestAttributes(request, event);

    event.begin();
    try {
      this.getHandler().handle(target, baseRequest, request, response);
      copyResponeAttributes(response, event);
    } finally {
      event.end();
      event.commit();
    }
  }

  private static void copyHttpRequestAttributes(HttpServletRequest request, HttpEvent event) {
    event.setMethod(request.getMethod());
    event.setUri(request.getRequestURI());
    event.setQuery(request.getQueryString());
    event.setDispatcherType(request.getDispatcherType().name());
  }

  private static void copyResponeAttributes(HttpServletResponse respone, HttpEvent event) {
    event.setStatus(respone.getStatus());
  }

  private void handleRelatedRequest(long exchangeId, String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    RelatedHttpEvent event = new RelatedHttpEvent();
    event.setExchangeId(exchangeId);
    event.setDispatcherType(request.getDispatcherType().name());
    try {
      this.getHandler().handle(target, baseRequest, request, response);
    } finally {
      event.end();
      event.commit();
    }
  }

  private static long generateExchangeId() {
    return EXCHANGE_ID_GENERATOR.incrementAndGet();
  }

  @Label("Exchange Id")
  @Description("Id to track requests that have been dispatched multiple times")
  @Relational
  @Target(FIELD)
  @Retention(RUNTIME)
  @interface ExchangeId {

  }

  @Label("related HTTP exchange")
  @Description("An HTTP exchange related to a different event")
  @Category("HTTP")
  @StackTrace(false)
  static class RelatedHttpEvent extends Event {

    @Label("Dispatcher Type")
    @Description("The dispatcher type of this request")
    private String dispatcherType;

    @ExchangeId
    private long exchangeId;

    String getDispatcherType() {
      return this.dispatcherType;
    }

    void setDispatcherType(String dispatcherType) {
      this.dispatcherType = dispatcherType;
    }

    long getExchangeId() {
      return this.exchangeId;
    }

    void setExchangeId(long exchangeId) {
      this.exchangeId = exchangeId;
    }

  }

  @Label("HTTP exchange")
  @Description("An HTTP exchange")
  @Category("HTTP")
  @StackTrace(false)
  static class HttpEvent extends Event {

    @Label("Method")
    @Description("The HTTP method")
    private String method;

    @Label("URI")
    @Description("The request URI")
    private String uri;

    @Label("Query")
    @Description("The query string")
    private String query;

    @Label("Status")
    @Description("The HTTP response status code")
    private int status;

    @Label("Dispatcher Type")
    @Description("The dispatcher type of this request")
    private String dispatcherType;

    @ExchangeId
    private long exchangeId;

    String getMethod() {
      return this.method;
    }

    void setMethod(String operationName) {
      this.method = operationName;
    }

    String getUri() {
      return this.uri;
    }

    void setUri(String query) {
      this.uri = query;
    }

    String getQuery() {
      return this.query;
    }

    void setQuery(String query) {
      this.query = query;
    }

    int getStatus() {
      return this.status;
    }

    void setStatus(int status) {
      this.status = status;
    }

    String getDispatcherType() {
      return this.dispatcherType;
    }

    void setDispatcherType(String dispatcherType) {
      this.dispatcherType = dispatcherType;
    }

    long getExchangeId() {
      return this.exchangeId;
    }

    void setExchangeId(long exchangeId) {
      this.exchangeId = exchangeId;
    }

  }

}
