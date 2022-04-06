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
package alpine.server.mail;

public class SendMailException extends Exception {

    /**
     * Constructs a new SendMailException with {@code null} as its detail message.
     * @since 1.3.0
     */
    public SendMailException() {
        super();
    }

    /**
     * Constructs a new SendMailException with the specified detail message.
     * @param message the detail message
     * @since 1.3.0
     */
    public SendMailException(final String message) {
        super(message);
    }

    /**
     * Constructs a new SendMailException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause of the exception
     * @since 1.3.0
     */
    public SendMailException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SendMailException with the specified cause.
     * @param cause the cause of the exception
     * @since 1.3.0
     */
    public SendMailException(final Throwable cause) {
        super(cause);
    }

}
