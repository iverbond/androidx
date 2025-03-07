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

package androidx.core.performance.play.services

import android.content.Context
import androidx.core.performance.DevicePerformanceRetriever
import com.google.android.gms.deviceperformance.DevicePerformanceClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * A DevicePerformanceRetriever that uses Google Play Services to retrieve media performance class data.
 *
 * @param context The application context value to use.
 */
class PlayServicesDevicePerformanceRetriever(val context: Context) : DevicePerformanceRetriever {
        override fun getPerformanceClass(): Int =
            PlayServicesDevicePerformanceRetriever.getPerformanceClass(
                context
            );

        companion object {
            @JvmStatic
            fun getPerformanceClass(context: Context): Int {
                val client: DevicePerformanceClient =
                    com.google.android.gms.deviceperformance.DevicePerformance.getClient(context)
                return runBlocking { client.mediaPerformanceClass().await() }
            }
        }
}
