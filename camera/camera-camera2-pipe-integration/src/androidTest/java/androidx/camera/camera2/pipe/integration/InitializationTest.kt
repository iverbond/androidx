/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration

import androidx.annotation.OptIn
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class InitializationTest {

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun initializeCameraX_withCameraPipeConfig() = runBlocking {
        ProcessCameraProvider.configureInstance(CameraPipeConfig.defaultConfig())

        // Retrieve camera provider initialized with CameraPipeConfig
        val cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext()).await()
        assertThat(cameraProvider).isNotNull()
        // Ensure retrieved provider is shut down
        cameraProvider.shutdown().await()
        return@runBlocking
    }
}
