/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.uwb.rxjava3.mock

import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import kotlinx.coroutines.flow.Flow

class TestUwbControllerSessionScope(
    private val uwbClient: TestUwbClient,
    override val rangingCapabilities: RangingCapabilities,
    override val localAddress: UwbAddress,
    override val uwbComplexChannel: UwbComplexChannel
) : UwbControllerSessionScope {
    private val uwbClientSessionScope =
        TestUwbClientSessionScope(uwbClient, rangingCapabilities, localAddress)
    override suspend fun addControlee(address: UwbAddress) {
        val uwbAddress = com.google.android.gms.nearby.uwb.UwbAddress(address.address)
        uwbClient.addControlee(uwbAddress)
    }

    override suspend fun removeControlee(address: UwbAddress) {
        val uwbAddress = com.google.android.gms.nearby.uwb.UwbAddress(address.address)
        uwbClient.removeControlee(uwbAddress)
    }

    override fun prepareSession(parameters: RangingParameters): Flow<RangingResult> {
        return uwbClientSessionScope.prepareSession(parameters)
    }
}
