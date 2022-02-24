/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.common.logging;

import org.slf4j.Marker;

/**
 * All logging is handled through this class. This class wraps an actual logging implementation
 * or logging framework so that implementations can be swapped out without having to modify
 * classes that use this logging mechanism.
 *
 * Note, if Markers are used, the logging implementation will be tied to SLF4j.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class Logger {

    /**
     * The logging framework being used.
     */
    private final org.slf4j.Logger log;

    /**
     * Create an instance of this class and initialize the underlying logging framework.
     * @param clazz The class to use when writing log information
     * @return An instance of the Logger class
     *
     * @since 1.0.0
     */
    public static Logger getLogger(final Class<?> clazz) {
        return new Logger(clazz);
    }

    /**
     * Create an instance of this class and initialize the underlying logging framework.
     * @param clazz The class to use when writing log information
     *
     * @since 1.0.0
     */
    private Logger(final Class<?> clazz) {
        log = org.slf4j.LoggerFactory.getLogger(clazz);
    }

    /**
     * Is the logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     * @since 1.0.0
     */
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    /**
     * Is the logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for the DEBUG level, false otherwise.
     * @since 1.0.0
     */
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    /**
     * Is the logger instance enabled for the ERROR level?
     *
     * @return True if this Logger is enabled for the ERROR level, false otherwise.
     * @since 1.0.0
     */
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    /**
     * Is the logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for the TRACE level, false otherwise.
     * @since 1.0.0
     */
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    /**
     * Is the logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level, false otherwise.
     * @since 1.0.0
     */
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    /**
     * Log a message at the INFO level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void info(final String message) {
        log.info(sanitize(message));
    }

    /**
     * Log a message at the INFO level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void info(final String message, final Throwable throwable) {
        log.info(sanitize(message), throwable);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void info(final Marker marker, final String message) {
        log.info(marker, sanitize(message));
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void info(final Marker marker, final String message, final Object object) {
        log.info(marker, sanitize(message), object);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param o1 the first argument
     * @param o2 the second argument
     * @since 1.0.0
     */
    public void info(final Marker marker, final String message, final Object o1, final Object o2) {
        log.info(marker, sanitize(message), o1, o1);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void info(final Marker marker, final String message, final Object... objects) {
        log.info(marker, sanitize(message), objects);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void info(final Marker marker, final String message, final Throwable throwable) {
        log.info(marker, sanitize(message), throwable);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void debug(final String message) {
        log.debug(sanitize(message));
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void debug(final String message, final Throwable throwable) {
        log.debug(sanitize(message), throwable);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void debug(final Marker marker, final String message) {
        log.debug(marker, sanitize(message));
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void debug(final Marker marker, final String message, final Object object) {
        log.debug(marker, sanitize(message), object);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param o1 the first argument
     * @param o2 the second argument
     * @since 1.0.0
     */
    public void debug(final Marker marker, final String message, final Object o1, final Object o2) {
        log.debug(marker, sanitize(message), o1, o1);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void debug(final Marker marker, final String message, final Object... objects) {
        log.debug(marker, sanitize(message), objects);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void debug(final Marker marker, final String message, final Throwable throwable) {
        log.debug(marker, sanitize(message), throwable);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void error(final String message) {
        log.error(sanitize(message));
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void error(final String message, final Throwable throwable) {
        log.error(sanitize(message), throwable);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void error(final Marker marker, final String message) {
        log.error(marker, sanitize(message));
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void error(final Marker marker, final String message, final Object object) {
        log.error(marker, sanitize(message), object);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param o1 the first argument
     * @param o2 the second argument
     * @since 1.0.0
     */
    public void error(final Marker marker, final String message, final Object o1, final Object o2) {
        log.error(marker, sanitize(message), o1, o1);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void error(final Marker marker, final String message, final Object... objects) {
        log.error(marker, sanitize(message), objects);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void error(final Marker marker, final String message, final Throwable throwable) {
        log.error(marker, sanitize(message), throwable);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void trace(final String message) {
        log.trace(sanitize(message));
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void trace(final String message, final Throwable throwable) {
        log.trace(sanitize(message), throwable);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void trace(final Marker marker, final String message) {
        log.trace(marker, sanitize(message));
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void trace(final Marker marker, final String message, final Object object) {
        log.trace(marker, sanitize(message), object);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param o1 the first argument
     * @param o2 the second argument
     * @since 1.0.0
     */
    public void trace(final Marker marker, final String message, final Object o1, final Object o2) {
        log.trace(marker, sanitize(message), o1, o1);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void trace(final Marker marker, final String message, final Object... objects) {
        log.trace(marker, sanitize(message), objects);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void trace(final Marker marker, final String message, final Throwable throwable) {
        log.trace(marker, sanitize(message), throwable);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void warn(final String message) {
        log.warn(sanitize(message));
    }

    /**
     * Log a message at the WARN level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void warn(final String message, final Throwable throwable) {
        log.warn(sanitize(message), throwable);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void warn(final Marker marker, final String message) {
        log.warn(marker, sanitize(message));
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void warn(final Marker marker, final String message, final Object object) {
        log.warn(marker, sanitize(message), object);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param o1 the first argument
     * @param o2 the second argument
     * @since 1.0.0
     */
    public void warn(final Marker marker, final String message, final Object o1, final Object o2) {
        log.warn(marker, sanitize(message), o1, o1);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void warn(final Marker marker, final String message, final Object... objects) {
        log.warn(marker, sanitize(message), objects);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void warn(final Marker marker, final String message, final Throwable throwable) {
        log.warn(marker, sanitize(message), throwable);
    }

    /**
     * Prevents possibility of CRLF injection.
     * @param message the message to sanitize
     * @return the sanitized message
     * @since 1.5.0
     */
    private String sanitize(String message) {
        return message.replaceAll("[\r\n]","");
    }
}
