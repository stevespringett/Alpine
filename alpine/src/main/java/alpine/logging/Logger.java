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
     * @since 1.0.0
     */
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    /**
     * @since 1.0.0
     */
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    /**
     * @since 1.0.0
     */
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    /**
     * @since 1.0.0
     */
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    /**
     * @since 1.0.0
     */
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    /**
     * @since 1.0.0
     */
    public void info(String string) {
        log.info(string);
    }

    /**
     * @since 1.0.0
     */
    public void info(String string, Throwable throwable) {
        log.info(string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void info(Marker marker, String string) {
        log.info(marker, string);
    }

    /**
     * @since 1.0.0
     */
    public void info(Marker marker, String string, Object object) {
        log.info(marker, string, object);
    }

    /**
     * @since 1.0.0
     */
    public void info(Marker marker, String string, Object o1, Object o2) {
        log.info(marker, string, o1, o1);
    }

    /**
     * @since 1.0.0
     */
    public void info(Marker marker, String string, Object... objects) {
        log.info(marker, string, objects);
    }

    /**
     * @since 1.0.0
     */
    public void info(Marker marker, String string, Throwable throwable) {
        log.info(marker, string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void debug(String string) {
        log.debug(string);
    }

    /**
     * @since 1.0.0
     */
    public void debug(String string, Throwable throwable) {
        log.debug(string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void debug(Marker marker, String string) {
        log.debug(marker, string);
    }

    /**
     * @since 1.0.0
     */
    public void debug(Marker marker, String string, Object object) {
        log.debug(marker, string, object);
    }

    /**
     * @since 1.0.0
     */
    public void debug(Marker marker, String string, Object o1, Object o2) {
        log.debug(marker, string, o1, o1);
    }

    /**
     * @since 1.0.0
     */
    public void debug(Marker marker, String string, Object... objects) {
        log.debug(marker, string, objects);
    }

    /**
     * @since 1.0.0
     */
    public void debug(Marker marker, String string, Throwable throwable) {
        log.debug(marker, string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void error(String string) {
        log.error(string);
    }

    /**
     * @since 1.0.0
     */
    public void error(String string, Throwable throwable) {
        log.error(string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void error(Marker marker, String string) {
        log.error(marker, string);
    }

    /**
     * @since 1.0.0
     */
    public void error(Marker marker, String string, Object object) {
        log.error(marker, string, object);
    }

    /**
     * @since 1.0.0
     */
    public void error(Marker marker, String string, Object o1, Object o2) {
        log.error(marker, string, o1, o1);
    }

    /**
     * @since 1.0.0
     */
    public void error(Marker marker, String string, Object... objects) {
        log.error(marker, string, objects);
    }

    /**
     * @since 1.0.0
     */
    public void error(Marker marker, String string, Throwable throwable) {
        log.error(marker, string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void trace(String string) {
        log.trace(string);
    }

    /**
     * @since 1.0.0
     */
    public void trace(String string, Throwable throwable) {
        log.trace(string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void trace(Marker marker, String string) {
        log.trace(marker, string);
    }

    /**
     * @since 1.0.0
     */
    public void trace(Marker marker, String string, Object object) {
        log.trace(marker, string, object);
    }

    /**
     * @since 1.0.0
     */
    public void trace(Marker marker, String string, Object o1, Object o2) {
        log.trace(marker, string, o1, o1);
    }

    /**
     * @since 1.0.0
     */
    public void trace(Marker marker, String string, Object... objects) {
        log.trace(marker, string, objects);
    }

    /**
     * @since 1.0.0
     */
    public void trace(Marker marker, String string, Throwable throwable) {
        log.trace(marker, string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void warn(String string) {
        log.warn(string);
    }

    /**
     * @since 1.0.0
     */
    public void warn(String string, Throwable throwable) {
        log.warn(string, throwable);
    }

    /**
     * @since 1.0.0
     */
    public void warn(Marker marker, String string) {
        log.warn(marker, string);
    }

    /**
     * @since 1.0.0
     */
    public void warn(Marker marker, String string, Object object) {
        log.warn(marker, string, object);
    }

    /**
     * @since 1.0.0
     */
    public void warn(Marker marker, String string, Object o1, Object o2) {
        log.warn(marker, string, o1, o1);
    }

    /**
     * @since 1.0.0
     */
    public void warn(Marker marker, String string, Object... objects) {
        log.warn(marker, string, objects);
    }

    /**
     * @since 1.0.0
     */
    public void warn(Marker marker, String string, Throwable throwable) {
        log.warn(marker, string, throwable);
    }

}
