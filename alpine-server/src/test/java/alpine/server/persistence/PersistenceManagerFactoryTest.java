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
package alpine.server.persistence;

import alpine.common.metrics.Metrics;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistenceManagerFactoryTest {

    @AfterEach
    void tearDown() {
        Metrics.getRegistry().getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("datanucleus_"))
                .toList()
                .forEach(Metrics.getRegistry()::remove);
    }

    @Test
    void shouldReportOverflowedDataNucleusCountersAsUnsigned() {
        final var pmfMock = mock(JDOPersistenceManagerFactory.class, RETURNS_DEEP_STUBS);
        final var statistics = pmfMock.getNucleusContext().getStatistics();
        when(statistics.getNumberOfDatastoreReads()).thenReturn(Integer.MIN_VALUE);
        when(statistics.getQueryExecutionTotalCount()).thenReturn(-1);

        new PersistenceManagerFactory().registerDataNucleusMetrics(pmfMock);

        assertThatNoException().isThrownBy(() -> Metrics.getRegistry().scrape());
        assertThat(Metrics.getRegistry().get("datanucleus_datastore_reads_total").functionCounter().count())
                .isEqualTo(2_147_483_648.0);
        assertThat(Metrics.getRegistry().get("datanucleus_queries_executed_total").functionCounter().count())
                .isEqualTo(4_294_967_295.0);
    }

}
