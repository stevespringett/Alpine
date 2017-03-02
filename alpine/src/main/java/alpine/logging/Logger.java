/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.logging;

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
    private org.slf4j.Logger log;

    /**
     * Create an instance of this class and initialize the underlying logging framework.
     * @param clazz The class to use when writing log information
     * @return An instance of the Logger class
     *
     * @since 1.0.0
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    /**
     * Create an instance of this class and initialize the underlying logging framework.
     * @param clazz The class to use when writing log information
     *
     * @since 1.0.0
     */
    private Logger(Class<?> clazz) {
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
    public void info(String message) {
        log.info(message);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void info(String message, Throwable throwable) {
        log.info(message, throwable);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void info(Marker marker, String message) {
        log.info(marker, message);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void info(Marker marker, String message, Object object) {
        log.info(marker, message, object);
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
    public void info(Marker marker, String message, Object o1, Object o2) {
        log.info(marker, message, o1, o1);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void info(Marker marker, String message, Object... objects) {
        log.info(marker, message, objects);
    }

    /**
     * Log a message at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void info(Marker marker, String message, Throwable throwable) {
        log.info(marker, message, throwable);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void debug(String message) {
        log.debug(message);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void debug(String message, Throwable throwable) {
        log.debug(message, throwable);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void debug(Marker marker, String message) {
        log.debug(marker, message);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void debug(Marker marker, String message, Object object) {
        log.debug(marker, message, object);
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
    public void debug(Marker marker, String message, Object o1, Object o2) {
        log.debug(marker, message, o1, o1);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void debug(Marker marker, String message, Object... objects) {
        log.debug(marker, message, objects);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void debug(Marker marker, String message, Throwable throwable) {
        log.debug(marker, message, throwable);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void error(String message) {
        log.error(message);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void error(Marker marker, String message) {
        log.error(marker, message);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void error(Marker marker, String message, Object object) {
        log.error(marker, message, object);
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
    public void error(Marker marker, String message, Object o1, Object o2) {
        log.error(marker, message, o1, o1);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void error(Marker marker, String message, Object... objects) {
        log.error(marker, message, objects);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void error(Marker marker, String message, Throwable throwable) {
        log.error(marker, message, throwable);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void trace(String message) {
        log.trace(message);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void trace(String message, Throwable throwable) {
        log.trace(message, throwable);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void trace(Marker marker, String message) {
        log.trace(marker, message);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void trace(Marker marker, String message, Object object) {
        log.trace(marker, message, object);
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
    public void trace(Marker marker, String message, Object o1, Object o2) {
        log.trace(marker, message, o1, o1);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void trace(Marker marker, String message, Object... objects) {
        log.trace(marker, message, objects);
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void trace(Marker marker, String message, Throwable throwable) {
        log.trace(marker, message, throwable);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void warn(String message) {
        log.warn(message);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void warn(String message, Throwable throwable) {
        log.warn(message, throwable);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @since 1.0.0
     */
    public void warn(Marker marker, String message) {
        log.warn(marker, message);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param object the argument
     * @since 1.0.0
     */
    public void warn(Marker marker, String message, Object object) {
        log.warn(marker, message, object);
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
    public void warn(Marker marker, String message, Object o1, Object o2) {
        log.warn(marker, message, o1, o1);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param objects a list of 3 or more arguments
     * @since 1.0.0
     */
    public void warn(Marker marker, String message, Object... objects) {
        log.warn(marker, message, objects);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param message the message string to be logged
     * @param throwable the exception (throwable) to log
     * @since 1.0.0
     */
    public void warn(Marker marker, String message, Throwable throwable) {
        log.warn(marker, message, throwable);
    }

}
