# logback-raygun


A logback appender that emits details to [raygun.io](http://raygun.io/).

## Configuration

````
<configuration>
  <appender name="RAYGUN" class="com.fatboyindustrial.raygun.RaygunAppender">
    <apiKey><!-- insert key here --></apiKey>

    <!-- Deny all events with a level below WARN, that is: TRACE, DEBUG, INFO. -->
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>WARN</level>
    </filter>
  </appender>

  <root level="DEBUG">
    <appender-ref ref="RAYGUN"/>
  </root>

</configuration>
````

Any `WARN` or `ERROR` log messages will be posted to raygun.  If the message contains an exception, then those exception details will be included.  If no exception is included, then the log message is reported, with a synthesised 1 element long stack trace of the log message call-site.

## Log with an exception

The line:

````
LOG.warn("oh no, not again",
    new RuntimeException(
        new UnsupportedOperationException("petunias can't fly", 
            new InputMismatchException())));
````

... produces:

**Message** oh no, not again; java.lang.RuntimeException: java.lang.UnsupportedOperationException: petunias can't fly; caused by: java.lang.UnsupportedOperationException: petunias can't fly; caused by: java.util.InputMismatchException

**Class Name** java.lang.RuntimeException

**Stack Trace**

````
oh no, not again; java.lang.RuntimeException: java.lang.UnsupportedOperationException: petunias can't fly; caused by: java.lang.UnsupportedOperationException: petunias can't fly; caused by: java.util.InputMismatchException
    com.fatboyindustrial.raygun.OtherClass.three in OtherClass.java:55
    com.fatboyindustrial.raygun.OtherClass.two in OtherClass.java:50
    com.fatboyindustrial.raygun.OtherClass.one in OtherClass.java:45
    com.fatboyindustrial.raygun.OtherClass.abc in OtherClass.java:40
    com.fatboyindustrial.raygun.RaygunTestApp.baz in RaygunTestApp.java:62
    com.fatboyindustrial.raygun.RaygunTestApp.bar in RaygunTestApp.java:57
    com.fatboyindustrial.raygun.RaygunTestApp.foo in RaygunTestApp.java:52
    com.fatboyindustrial.raygun.RaygunTestApp.entry in RaygunTestApp.java:47
    com.fatboyindustrial.raygun.RaygunTestApp.main in RaygunTestApp.java:41

Caused by; java.lang.UnsupportedOperationException: petunias can't fly; caused by: java.util.InputMismatchException
    com.fatboyindustrial.raygun.OtherClass.three in OtherClass.java:55
    com.fatboyindustrial.raygun.OtherClass.two in OtherClass.java:50
    com.fatboyindustrial.raygun.OtherClass.one in OtherClass.java:45
    com.fatboyindustrial.raygun.OtherClass.abc in OtherClass.java:40
    com.fatboyindustrial.raygun.RaygunTestApp.baz in RaygunTestApp.java:62
    com.fatboyindustrial.raygun.RaygunTestApp.bar in RaygunTestApp.java:57
    com.fatboyindustrial.raygun.RaygunTestApp.foo in RaygunTestApp.java:52
    com.fatboyindustrial.raygun.RaygunTestApp.entry in RaygunTestApp.java:47
    com.fatboyindustrial.raygun.RaygunTestApp.main in RaygunTestApp.java:41

Caused by; java.util.InputMismatchException
    com.fatboyindustrial.raygun.OtherClass.three in OtherClass.java:55
    com.fatboyindustrial.raygun.OtherClass.two in OtherClass.java:50
    com.fatboyindustrial.raygun.OtherClass.one in OtherClass.java:45
    com.fatboyindustrial.raygun.OtherClass.abc in OtherClass.java:40
    com.fatboyindustrial.raygun.RaygunTestApp.baz in RaygunTestApp.java:62
    com.fatboyindustrial.raygun.RaygunTestApp.bar in RaygunTestApp.java:57
    com.fatboyindustrial.raygun.RaygunTestApp.foo in RaygunTestApp.java:52
    com.fatboyindustrial.raygun.RaygunTestApp.entry in RaygunTestApp.java:47
    com.fatboyindustrial.raygun.RaygunTestApp.main in RaygunTestApp.java:41

````

## Log without an exception

The line:

````
LOG.warn("I hope it will be friends with me.");
````

... produces:

**Message** I hope it will be friends with me.

**Class Name** com.fatboyindustrial.raygun.OtherClass

**Stack Trace**

````
I hope it will be friends with me.
    com.fatboyindustrial.raygun.OtherClass.three in OtherClass.java:53
````
