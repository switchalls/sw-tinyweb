# Tiny web server

A simple HTTP 1.1 server designed for use on low powered embedded platforms.

## Building

```bash
mvn clean install
```

## Run standalone server

```bash
$ java -jar target/sw-tinyweb-0.0.1-SNAPSHOT-jar-with-dependencies.jar 8080 ./WebContent

INFO  [main, Main] contextInitialized(javax.servlet.ServletContextEvent[source=sw.tinyweb.TinyWebServletContext@9807454])
INFO  [main, TinyWebServer] Starting web server on port 8080
```

Runs `tinyweb` using `sw.tinyweb.standalone.Main`

## Testing

* http://localhost:8080/about

  ```
  TinyWeb - HTTP/1.1 javax.servlet container
  (c) Stewart Witchalls 2012
  ```

* http://localhost:8080/js/ajaxutils.js

  ```javascript
  /*--------------------------------------------------|
   | AJAX Utilities   |   www.javascript.about.com    |
   |--------------------------------------------------|
   | Copyright (c) 2007 javascript.about.com          |
   |                                                  |
   | This script can be used freely as long as all    |
   | copyright messages are intact.                   |
   |                                                  |
   | Updated: 03.11.2008                              |
   |--------------------------------------------------*/
  ```

* http://localhost:8080/w3org.html
