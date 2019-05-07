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
package alpine.event.framework;

/**
 * The UnblockedEvent interface extends an Event and is treated a bit differently as a result.
 * An UnblockedEvent will always be able to scale its parallelism to the number of cores available
 * where as an Event will use a configurable number of threads.
 *
 * Use of this interface should be taken with caution. It is recommended to only use this when
 * a simple, non-blocking task needs to be done asap and there is a possibility that the path taken
 * for typical Events may be at capacity.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public interface UnblockedEvent extends Event {

}
