/*
 *  Copyright 2014-2015 Greg Kopff
 *  All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.fatboyindustrial.raygun;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.RaygunMessageBuilder;
import com.mindscapehq.raygun4java.core.messages.RaygunErrorMessage;
import com.mindscapehq.raygun4java.core.messages.RaygunErrorStackTraceLineMessage;
import com.mindscapehq.raygun4java.core.messages.RaygunMessage;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

/**
 * A logback appender that emits details to {@code raygun.io}.
 */
public class RaygunAppender extends AppenderBase<ILoggingEvent>
{
  /** System property name for the application ID. */
  public static final String PROPERTY_APPLICATION_ID =
      "com.fatboyindustrial.raygun.UserCustomData.applicationId";

  /** The name of our Raygun submission software. */
  private static final String NAME = "logback-raygun";

  /** The version of our Raygun submission software. */
  private static final String VERSION = "1.3.0";

  /** The URL of our Raygun submission software. */
  private static final String URL = "https://github.com/gkopff/logback-raygun";

  /** The RayGun API key master. */
  private KeyMaster keyMaster;

  /** Tags to send to Raygun. */
  private List<String> tags = ImmutableList.of();

  /**
   * No-arg constructor required by Logback.
   */
  public RaygunAppender()
  {
    ;
  }

  /**
   * Appends the logging event.
   * @param e The logging event.
   */
  @Override
  protected void append(ILoggingEvent e)
  {
    final String host = getMachineName();
    final Optional<String> apiKey = this.keyMaster.getApiKey(host);

    if (apiKey.isPresent())
    {
      final RaygunClient ray = new RaygunClient(apiKey.get());

      // We use the Raygun supplied classes a bit ... but we customise.

      final RaygunMessage msg = RaygunMessageBuilder.New()
          .SetEnvironmentDetails()
          .SetMachineName(host)
          .SetClientDetails()
          .Build();
      msg.getDetails().getClient().setName(NAME);
      msg.getDetails().getClient().setVersion(VERSION);
      msg.getDetails().getClient().setClientUrlString(URL);
      msg.getDetails().setError(buildRaygunMessage(e));
      msg.getDetails().setTags(tags);
      
      Map<String, String> customData = Maps.newHashMap();
      customData.put("thread", e.getThreadName());
      customData.put("logger", e.getLoggerName());
      customData.put("applicationId", System.getProperty(PROPERTY_APPLICATION_ID, "unnamed"));
      customData.put("datetime", new Date(e.getTimeStamp()).toString());
      Map<String, String> mdcContext = e.getMDCPropertyMap();
      for(String mdcKey : mdcContext.keySet()) {
          customData.put("mdc:" + mdcKey, mdcContext.get(mdcKey));
      }
      
      msg.getDetails().setUserCustomData(ImmutableMap.copyOf(customData));

      ray.Post(msg);
    }
  }

  /**
   * Sets the API key.
   * @param apiKey The API key.
   */
  @SuppressWarnings("UnusedDeclaration")         // called by slf4j
  public void setApiKey(String apiKey)
  {
    Preconditions.checkNotNull(apiKey, "apiKey cannot be null");

    this.keyMaster = KeyMaster.fromConfigString(apiKey);
  }

  /**
   * Sets the tags.
   * @param tags The tags.
   */
  @SuppressWarnings("UnusedDeclaration") // called by slf4j
  public void setTags(String tags)
  {
    Preconditions.checkNotNull(tags, "tags cannot be null");

    this.tags = ImmutableList.copyOf(tags.split(","));
  }

  /**
   * Builds a {@code RaygunErrorMessage} for the given logging event.
   * @param loggingEvent The logging event.
   * @return The raygun message.
   */
  private static RaygunErrorMessage buildRaygunMessage(ILoggingEvent loggingEvent)
  {
    final Optional<IThrowableProxy> exception = Optional.fromNullable(loggingEvent.getThrowableProxy());
    return buildRaygunMessage(loggingEvent.getFormattedMessage(), exception);
  }

  /**
   * Builds a raygun message based on the given log message and optional exception details.
   * @param message The log message (fully formatted).
   * @param exception The optional exception details.
   * @return The raygun message.
   */
  private static RaygunErrorMessage buildRaygunMessage(String message, Optional<IThrowableProxy> exception)
  {
    // The Raygun error message constructor wants a real exception, which we don't have - we only have
    // a logback throwable proxy.  Therefore, we construct the error message with any old exception,
    // then make a second pass to fill in the real values.

    final RaygunErrorMessage error = new RaygunErrorMessage(new Exception());
    final String className;
    final RaygunErrorStackTraceLineMessage[] trace;
    final Optional<RaygunErrorMessage> inner;
    final Optional<String> appId = Optional.fromNullable(System.getProperty(PROPERTY_APPLICATION_ID));
    final StringBuilder buff = new StringBuilder();

    if (appId.isPresent())
    {
      buff.append(appId.get()).append(": ");
    }
    buff.append(message);

    if (exception.isPresent())
    {
      buff.append("; ");
      buff.append(buildCausalString(exception.get()));

      className = exception.get().getClassName();
      trace = buildRaygunStack(exception.get());

      if (exception.get().getCause() != null)
      {
        inner = Optional.of(buildRaygunMessage("Caused by", Optional.of(exception.get().getCause())));
      }
      else
      {
        inner = Optional.absent();
      }
    }
    else
    {
      trace = new RaygunErrorStackTraceLineMessage[] { new RaygunErrorStackTraceLineMessage(locateCallSite()) };
      className = trace[0].getClassName();
      inner = Optional.absent();
    }

    error.setMessage(buff.toString());
    error.setClassName(className);
    error.setStackTrace(trace);

    if (inner.isPresent())
    {
      error.setInnerError(inner.get());
    }

    return error;
  }

  /**
   * Builds an exception causation string by following the exception caused-by chain.
   * @param exception The exception to process.
   * @return A string describing all exceptions in the chain.
   */
  private static String buildCausalString(IThrowableProxy exception)
  {
    final StringBuilder buff = new StringBuilder();

    buff.append(exception.getClassName());
    if (exception.getMessage() != null)
    {
      buff.append(": ").append(exception.getMessage());
    }

    if (exception.getCause() != null)
    {
      buff.append("; caused by: ").append(buildCausalString(exception.getCause()));
    }

    return buff.toString();
  }

  /**
   * Builds a raygun stack trace from the given logback throwable proxy object.
   * @param throwableProxy The logback throwable proxy.
   * @return The raygun stack trace information.
   */
  private static RaygunErrorStackTraceLineMessage[] buildRaygunStack(IThrowableProxy throwableProxy)
  {
    final StackTraceElementProxy[] proxies = throwableProxy.getStackTraceElementProxyArray();
    final RaygunErrorStackTraceLineMessage[] lines = new RaygunErrorStackTraceLineMessage[proxies.length];

    for (int i = 0; i < proxies.length; i++)
    {
      final StackTraceElementProxy step = proxies[i];
      lines[i] = new RaygunErrorStackTraceLineMessage(step.getStackTraceElement());
    }

    return lines;
  }

  /**
   * Gets the machine's hostname.
   * @return The hostname, or "UnknownHost" if it cannot be determined.
   */
  private static String getMachineName()
  {
    try
    {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e)
    {
      return "UnknownHost";
    }
  }

  /**
   * Finds the stack trace elements that corresponds to the actual log call-site.
   * @return The applicable stack trace element.
   */
  private static StackTraceElement locateCallSite()
  {
    // The stack will contain Fat Boy Industrial entries, followed by logback entries,
    // and then the actual call-site ...

    final String FBI = "com.fatboyindustrial.raygun.RaygunAppender";
    final String LOGBACK = "ch.qos.logback.";

    for (StackTraceElement ste : new Exception().getStackTrace())
    {
      if (ste.getClassName().startsWith(FBI) ||
          ste.getClassName().startsWith(LOGBACK))
      {
        continue;
      }

      return ste;
    }

    throw new IllegalStateException("Unable to determine call-site");
  }
}
