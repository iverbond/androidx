/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.core.performance

import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Reports the media performance class of the device. Contains statically specified values
 * and can be used as a fallback alternative to suppliers with dynamic values.
 */
class StaticDevicePerformanceSupplier : DevicePerformanceSupplier {

    companion object {
        /**
         * Create DevicePerformance from the context backed by StaticDevicePerformanceSupplier.
         *
         * This should be done in [android.app.Application.onCreate].
         */
        @JvmStatic
        fun createDevicePerformance(): DevicePerformance =
            DevicePerformance.create(StaticDevicePerformanceSupplier())
    }

    override val mediaPerformanceClassFlow: Flow<Int> = flow {
        emit(calculateMediaPerformanceClass())
    }

    // the next code isn't published/finalized until late
    private val PERFCLASS_11: Int = Build.VERSION_CODES.R
    private val PERFCLASS_12: Int = Build.VERSION_CODES.S
    private val PERFCLASS_13: Int = 33

    // we synthesize our own fingerprint:
    //       Brand/Product/Device:api
    // this is currently the prefix in the actual build fingerprints,
    // so it is easy to cut&paste info from there into this list.
    //
    private val fingerprints: HashMap<String, Int> = hashMapOf(
        // for unit testing, no actual products with these
        "robolectric-BrandX/ProductX/Device30:11" to PERFCLASS_11,
        "robolectric-BrandX/ProductX/Device31:12" to PERFCLASS_12,

        // actual devices in the field
        "OPPO/CPH2025EEA/OP4BA2L1:12" to PERFCLASS_11,
        "OPPO/CPH2207EEA/OP4F0BL1:12" to PERFCLASS_11,
        "OPPO/PENM00/OP4EC1:11" to PERFCLASS_11,
        "OnePlus/OnePlus7TTMO/OnePlus7TTMO:11" to PERFCLASS_11,
        "OnePlus/OnePlus8_BETA/OnePlus8:11" to PERFCLASS_11,
        "Xiaomi/umi_global/umi:11" to PERFCLASS_11,
        "realme/RMX2085/RMX2085L1:11" to PERFCLASS_11,
        "samsung/c1qsqw/c1q:12" to PERFCLASS_11,
        "samsung/o1quew/o1q:12" to PERFCLASS_11,
        "samsung/r0quew/r0q:12" to PERFCLASS_11,
        "samsung/r0sxxx/r0s:12" to PERFCLASS_11,
    )

    private fun calculateMediaPerformanceClassFromFingerprint(): Int {

        val brand = Build.BRAND
        val product = Build.PRODUCT
        val device = Build.DEVICE
        val release = Build.VERSION.RELEASE

        val synthesized = "$brand/$product/$device:$release"
        var pc = fingerprints[synthesized]

        if (pc == null) {
            pc = 0
        }
        return pc
    }

    private fun calculateMediaPerformanceClassFromProperty(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Build.VERSION.MEDIA_PERFORMANCE_CLASS
        }
        return 0
    }

    // discard illegal performance class numbers
    private fun boundPC(pc: Int): Int {
        if (pc < PERFCLASS_11) {
            // performance class 11 is the first legal value for performance class
            return 0
        }
        return pc
    }

    private fun calculateMediaPerformanceClass(): Int {

        // device's declared property takes precedence over our in-library table.
        //
        // what this flow doesn't let us express is a device with property PC11
        // to actually upgraded to PC12 via the table.
        //
        val viaProperty: Int = boundPC(calculateMediaPerformanceClassFromProperty())
        if (viaProperty > 0) {
            return viaProperty
        }
        val viaFingerprint: Int = calculateMediaPerformanceClassFromFingerprint()
        return boundPC(viaFingerprint)
    }
}
