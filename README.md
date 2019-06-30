
Jetty JFR
=========

A Catalina [handler](https://www.eclipse.org/jetty/documentation/9.4.x/architecture.html#_handlers) that generates [Flight Recorder](https://openjdk.java.net/jeps/328) events. Unlike a filter based approach a valve based approach also generate events for internal servlets like the `DefaultServlet` and `JasperServlet`. This valve can correlate multiple async events that belong to the same original HTTP request.


This project requires Java 11.

