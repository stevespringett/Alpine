/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.logging;

/**
 * All logging is handled through this class. This class wraps an actual logging implementation
 * or logging framework so that implementations can be swapped out without having to modify
 * classes that use this logging mechanism.
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

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public void info(String string) {
        log.info(string);
    }

    public void info(String string, Throwable throwable) {
        log.info(string, throwable);
    }

    public void debug(String string) {
        log.debug(string);
    }

    public void debug(String string, Throwable throwable) {
        log.debug(string, throwable);
    }

    public void error(String string) {
        log.error(string);
    }

    public void error(String string, Throwable throwable) {
        log.error(string, throwable);
    }

    public void trace(String string) {
        log.trace(string);
    }

    public void trace(String string, Throwable throwable) {
        log.trace(string, throwable);
    }

    public void warn(String string) {
        log.warn(string);
    }

    public void warn(String string, Throwable throwable) {
        log.warn(string, throwable);
    }

}
