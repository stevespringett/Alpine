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
package alpine.auth;

import javax.naming.AuthenticationException;

/**
 * An exception class that optionally holds pre-determined causes for common
 * authentication failures.
 *
 * @author Steve Springett
 * @since 1.0.1
 */
public class AlpineAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = 7367893115241461285L;

    public enum CauseType {
        INVALID_CREDENTIALS,
        EXPIRED_CREDENTIALS,
        FORCE_PASSWORD_CHANGE,
        SUSPENDED,
        UNMAPPED_ACCOUNT,
        OTHER
    }

    private CauseType causeType;

    public AlpineAuthenticationException(CauseType causeType) {
        super();
        this.causeType = causeType;
    }

    public AlpineAuthenticationException(CauseType causeType, String explanation) {
        super(explanation);
        this.causeType = causeType;
    }

    public CauseType getCauseType() {
        return causeType;
    }

}
