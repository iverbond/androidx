/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.performance.testing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Unit tests for [FakeDevicePerformanceSupplier]. */
class FakeDevicePerformanceSupplierTest {

    @Test
    fun mediaPerformanceClassFlow_30() {
        // TODO: b/289279260 - Correctly handle threads in tests without leaking.
        assertThat(true).isEqualTo(true)
    }
}
