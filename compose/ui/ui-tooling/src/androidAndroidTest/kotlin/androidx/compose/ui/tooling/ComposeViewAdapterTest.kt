/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.tooling

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.PreviewAnimationClock
import androidx.compose.ui.tooling.animation.UnsupportedComposeAnimation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.test.R
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
@OptIn(UiToolingDataApi::class)
class ComposeViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    private lateinit var composeViewAdapter: ComposeViewAdapter

    @Before
    fun setup() {
        composeViewAdapter = activityTestRule.activity.findViewById(R.id.compose_view_adapter)
    }

    /**
     * Asserts that the given Composable method executes correct and outputs some [ViewInfo]s.
     */
    private fun assertRendersCorrectly(className: String, methodName: String): List<ViewInfo> {
        initAndWaitForDraw(className, methodName)
        activityTestRule.runOnUiThread {
            assertTrue(composeViewAdapter.viewInfos.isNotEmpty())
        }

        return composeViewAdapter.viewInfos
    }

    /**
     * Initiates the given Composable method and waits for the [ComposeViewAdapter.onDraw] callback.
     */
    private fun initAndWaitForDraw(
        className: String,
        methodName: String,
        designInfoProvidersArgument: String? = null
    ) {
        val committedAndDrawn = CountDownLatch(1)
        val committed = AtomicBoolean(false)
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                className,
                methodName,
                debugViewInfos = true,
                lookForDesignInfoProviders = true,
                designInfoProvidersArgument = designInfoProvidersArgument,
                onCommit = {
                    committed.set(true)
                },
                onDraw = {
                    if (committed.get()) {
                        committedAndDrawn.countDown()
                    }
                }
            )
        }

        // Workaround for a problem described in b/174291742 where onLayout will not be called
        // after composition for the first test in the test suite.
        activityTestRule.runOnUiThread {
            composeViewAdapter.requestLayout()
        }

        // Wait for the first draw after the Composable has been committed.
        committedAndDrawn.await()
    }

    @Test
    fun instantiateComposeViewAdapter() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "SimpleComposablePreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "SimpleComposablePreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // Verify that valid line numbers are being recorded
            assertTrue(viewInfos.map { it.lineNumber }.all { it > 0 })
            // Verify that this composable has no animations
            assertFalse(composeViewAdapter.hasAnimations())
        }
    }

    @Test
    fun lazyColumn() {
        run {
            composeViewAdapter.stitchTrees = false
            val viewInfos = assertRendersCorrectly(
                "androidx.compose.ui.tooling.LazyColumnPreviewKt",
                "SimpleLazyComposablePreview"
            )

            assertEquals(
                """
                    |<root>
                    .|LazyColumnPreview.kt:35
                    |<root>
                    .|LazyColumnPreview.kt:35
                    |<root>
                    .|LazyColumnPreview.kt:35
                    |<root>
                    .|LazyColumnPreview.kt:31
                    ..|LazyColumnPreview.kt:32
                """.trimIndent(),
                viewInfos.toDebugString {
                it.fileName == "LazyColumnPreview.kt"
            }.trimIndent())
        }

        run {
            composeViewAdapter.stitchTrees = true
            val viewInfos = assertRendersCorrectly(
                "androidx.compose.ui.tooling.LazyColumnPreviewKt",
                "SimpleLazyComposablePreview"
            )

            assertEquals(
                1,
                viewInfos.size
            )
            assertEquals(
                """
                    |<root>
                    .|LazyColumnPreview.kt:31
                    ..|LazyColumnPreview.kt:32
                    ...|LazyColumnPreview.kt:35
                    ...|LazyColumnPreview.kt:35
                    ...|LazyColumnPreview.kt:35
                """.trimIndent(),
                viewInfos.toDebugString() {
                    it.fileName == "LazyColumnPreview.kt"
                }.trimIndent())
        }
    }

    @Test
    fun complexTreeStitchLazyColumn() {
        run {
            composeViewAdapter.stitchTrees = true
            val viewInfos = assertRendersCorrectly(
                "androidx.compose.ui.tooling.LazyColumnPreviewKt",
                "ComplexLazyComposablePreview"
            )

            assertEquals(1, viewInfos.size)
            assertEquals(
                """
                    |<root>
                    .|LazyColumnPreview.kt:45
                    ..|LazyColumnPreview.kt:46
                    ...|LazyColumnPreview.kt:49
                    ...|LazyColumnPreview.kt:50
                    ....|LazyColumnPreview.kt:53
                    .....|LazyColumnPreview.kt:54
                    ....|LazyColumnPreview.kt:53
                    .....|LazyColumnPreview.kt:54
                """.trimIndent(),
                viewInfos.toDebugString() {
                    it.fileName == "LazyColumnPreview.kt"
                }.trimIndent()
            )
        }
    }

    @Test
    fun animatedVisibilityIsTracked() {
        val clock = PreviewAnimationClock()

        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestAnimationPreviewKt",
                "AnimatedVisibilityPreview"
            )
            composeViewAdapter.clock = clock
            assertFalse(composeViewAdapter.hasAnimations())
            assertTrue(clock.animatedVisibilityClocks.isEmpty())
        }

        waitFor(1, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.hasAnimations()
        }

        activityTestRule.runOnUiThread {
            val animation = clock.animatedVisibilityClocks.values.single().animation
            assertEquals("My Animated Visibility", animation.label)
        }
    }

    @Test
    fun transitionAnimatedVisibilityIsTrackedAsTransition() {
        checkAnimationsAreSubscribed(
            "TransitionAnimatedVisibilityPreview",
            emptyList(),
            listOf("transition.AV")
        )
    }

    @Test
    fun animatedContentIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimatedContentPreview",
            animatedContent = listOf("AnimatedContent")
        )
    }

    @Test
    fun animatedContentAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimatedContentAndTransitionPreview",
            transitions = listOf("checkBoxAnim"),
            animatedContent = listOf("AnimatedContent")
        )
    }

    @Test
    fun transitionAnimationsAreSubscribedToTheClock() {
        checkAnimationsAreSubscribed(
            "TransitionPreview",
            emptyList(),
            listOf("checkBoxAnim")
        )
    }

    @Test
    fun transitionAnimationsWithSubcomposition() {
        checkAnimationsAreSubscribed(
            "TransitionWithScaffoldPreview",
            emptyList(),
            listOf("checkBoxAnim")
        )
    }

    @Test
    fun animateXAsStateIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimateAsStatePreview",
            animateXAsState = listOf("DpAnimation", "IntAnimation")
        )
    }

    @Test
    fun animateXAsStateIsNotSubscribed() {
        AnimateXAsStateComposeAnimation.testOverrideAvailability(false)
        checkAnimationsAreSubscribed(
            "AllAnimations",
            unsupported = listOf(
                "animateContentSize",
                "TargetBasedAnimation",
                "DecayAnimation",
            ),
            transitions = listOf("checkBoxAnim", "Crossfade"),
            animatedContent = listOf("AnimatedContent"),
            animateXAsState = emptyList(),
            infiniteTransitions = listOf("InfiniteTransition")
        )
        AnimateXAsStateComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun animateContentSizeIsNotSubscribed() {
        checkAnimationsAreSubscribed("AnimateContentSizePreview")
    }

    @Test
    fun animateContentSizeAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimateContentSizeAndTransitionPreview",
            listOf("animateContentSize"),
            listOf("checkBoxAnim")
        )
    }

    @Test
    fun crossFadeIsSubscribed() {
        checkAnimationsAreSubscribed(
            "CrossFadePreview",
            emptyList(),
            listOf("Crossfade")
        )
    }

    @Test
    fun targetBasedAnimationIsNotSubscribed() {
        checkAnimationsAreSubscribed("TargetBasedAnimationPreview")
    }

    @Test
    fun decayAnimationIsNotSubscribed() {
        checkAnimationsAreSubscribed("DecayAnimationPreview")
    }

    @Test
    fun infiniteTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "InfiniteTransitionPreview",
            infiniteTransitions = listOf("InfiniteTransition")
        )
    }

    @Test
    fun targetBasedAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "TargetBasedAndTransitionPreview",
            listOf("TargetBasedAnimation"),
            listOf("checkBoxAnim")
        )
    }

    @Test
    fun decayAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "DecayAndTransitionPreview",
            listOf("DecayAnimation"),
            listOf("checkBoxAnim")
        )
    }

    @Test
    fun infiniteAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "InfiniteAndTransitionPreview",
            transitions = listOf("checkBoxAnim"),
            infiniteTransitions = listOf("InfiniteTransition")
        )
    }

    @Test
    fun unsupportedAreNotSubscribedWhenEnumIsNotAvailable() {
        UnsupportedComposeAnimation.testOverrideAvailability(false)
        checkAnimationsAreSubscribed(
            "AllAnimations",
            unsupported = emptyList(),
            transitions = listOf("checkBoxAnim", "Crossfade"),
            animateXAsState = listOf("DpAnimation", "IntAnimation"),
            animatedContent = listOf("AnimatedContent"),
            infiniteTransitions = listOf("InfiniteTransition")
        )
        UnsupportedComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun animationsAreOrdered() {
        checkAnimationsAreSubscribed(
            "AnimationOrder",
            emptyList(),
            listOf("transitionOne", "transitionTwo", "transitionThree")
        )
    }

    @Test
    fun materialAnimationsAreSubscribed() {
        checkAnimationsAreSubscribed(
            "MaterialPreview",
            unsupported = emptyList(),
            transitions = listOf("ToggleableState"),
            animateXAsState = listOf("ColorAnimation", "ColorAnimation", "ColorAnimation")
        )
    }

    private fun checkAnimationsAreSubscribed(
        preview: String,
        unsupported: List<String> = emptyList(),
        transitions: List<String> = emptyList(),
        animateXAsState: List<String> = emptyList(),
        animatedContent: List<String> = emptyList(),
        infiniteTransitions: List<String> = emptyList()
    ) {
        val clock = PreviewAnimationClock()

        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestAnimationPreviewKt",
                preview
            )
            composeViewAdapter.clock = clock
            assertFalse(composeViewAdapter.hasAnimations())
            assertTrue(clock.transitionClocks.isEmpty())
            assertTrue(clock.trackedUnsupportedAnimations.isEmpty())
            assertTrue(clock.animatedVisibilityClocks.isEmpty())
            assertTrue(clock.animatedContentClocks.isEmpty())
            assertTrue(clock.infiniteTransitionClocks.isEmpty())
        }

        waitFor(5, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.hasAnimations()
        }

        activityTestRule.runOnUiThread {
            assertEquals(unsupported, clock.trackedUnsupportedAnimations.map { it.label })
            assertEquals(transitions, clock.transitionClocks.values.map { it.animation.label })
            assertEquals(animateXAsState,
                clock.animateXAsStateClocks.values.map { it.animation.label })
            assertEquals(
                animatedContent,
                clock.animatedContentClocks.values.map { it.animation.label })
            assertEquals(infiniteTransitions,
                clock.infiniteTransitionClocks.values.map { it.animation.label })
            assertEquals(0, clock.animatedVisibilityClocks.size)
        }
    }

    @Test
    fun lineNumberMapping() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.LineNumberPreviewKt",
            "LineNumberPreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "LineNumberPreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            assertArrayEquals(
                arrayOf(36, 37, 38, 40, 43, 44, 45),
                viewInfos
                    .map { it.lineNumber }
                    .sorted()
                    .distinct()
                    .toTypedArray()
            )
        }
    }

    //    @Test
    fun lineNumberLocationMapping() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.LineNumberPreviewKt",
            "LineNumberPreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.location?.let { it.sourceFile == "LineNumberPreview.kt" } == true }
            .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            val lines = viewInfos
                .map { it.location?.lineNumber ?: -1 }
                .sorted()
                .toTypedArray()
            assertArrayEquals(arrayOf(36, 37, 38, 40, 40, 40, 43, 44, 44, 45, 45), lines)

            // Verify that all calls generate the correct offset information
            val offsets = viewInfos
                .map { it.location?.offset ?: -1 }
                .sorted()
                .toTypedArray()
            assertArrayEquals(
                arrayOf(1235, 1272, 1293, 1421, 1421, 1421, 1469, 1491, 1508, 1531, 1548),
                offsets
            )
        }
    }

    @Test
    fun instantiatePrivateComposeViewAdapter() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "PrivateSimpleComposablePreview"
        )
    }

    @Test
    fun defaultParametersComposableTest1() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview1"
        )
    }

    @Test
    fun defaultParametersComposableTest2() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview2"
        )
    }

    @Test
    fun defaultParametersComposableTest3() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview3"
        )
    }

    /**
     * Verifies the use of inline classes as preview default parameters. Methods with inline
     * classes as parameters will get the name mangled so we need to ensure we invoke correctly
     * the right method.
     */
    @Test
    fun defaultParametersComposableTest4() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview4"
        )
    }

    @Test
    fun PreviewParametersComposableTest() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "PreviewParametersComposablePreview"
        )
    }

    @Test
    fun previewInClass() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.TestGroup",
            "InClassPreview"
        )
    }

    @Test
    fun lifecycleUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "LifecyclePreview"
        )
    }

    @Test
    fun saveableStateRegistryUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "SaveableStateRegistryPreview"
        )
    }

    @Test
    fun onBackPressedDispatcherUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "OnBackPressedDispatcherPreview"
        )
    }

    @Test
    fun activityResultRegistryUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "ActivityResultRegistryPreview"
        )
    }

    @Test
    fun viewModelPreviewRendersCorrectly() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "ViewModelPreview"
        )
    }

    @Test
    fun multipreviewTest() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "Multipreview"
        )
        assertRendersCorrectly(
                "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                "MultiPreviews"
        )
    }

    /**
     * Check that no re-composition happens without forcing it.
     */
    @LargeTest
    @Test
    fun testNoInvalidation() {
        compositionCount.set(0)
        var onDrawCounter = 0
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestInvalidationPreviewKt",
                "CounterPreview",
                forceCompositionInvalidation = false,
                onDraw = { onDrawCounter++ }
            )
        }

        // API before 29, might issue an additional draw under testing.
        val expectedDrawCount = if (Build.VERSION.SDK_INT < 29) 2 else 1
        repeat(5) {
            activityTestRule.runOnUiThread {
                assertEquals(1, compositionCount.get())
                assertTrue(
                    "At most, $expectedDrawCount draw is expected ($onDrawCounter happened)",
                    onDrawCounter <= expectedDrawCount
                )
            }
            Thread.sleep(250)
        }
    }

    /**
     * Check re-composition happens when forced.
     */
    @Test
    fun testInvalidation() {
        compositionCount.set(0)
        val drawCountDownLatch = CountDownLatch(10)
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestInvalidationPreviewKt",
                "CounterPreview",
                forceCompositionInvalidation = true,
                onDraw = { drawCountDownLatch.countDown() }
            )
        }
        activityTestRule.runOnUiThread {
            assertEquals(1, compositionCount.get())
        }
        // Draw will keep happening so, eventually this will hit 0
        assertTrue(drawCountDownLatch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun simpleDesignInfoProviderTest() {
        checkDesignInfoList("DesignInfoProviderA", "A", "ObjectA, x=0, y=0")
        checkDesignInfoList("DesignInfoProviderA", "B", "Invalid, x=0, y=0")

        checkDesignInfoList("DesignInfoProviderB", "A", "Invalid, x=0, y=0")
        checkDesignInfoList("DesignInfoProviderB", "B", "ObjectB, x=0, y=0")
    }

    @Test
    fun subcompositionDesignInfoProviderTest() {
        checkDesignInfoList("ScaffoldDesignInfoProvider", "A", "ObjectA, x=0, y=0")
    }

    private fun checkDesignInfoList(
        methodName: String,
        customArgument: String,
        expectedResult: String
    ) {
        initAndWaitForDraw(
            "androidx.compose.ui.tooling.DesignInfoProviderComposableKt",
            methodName,
            customArgument
        )

        activityTestRule.runOnUiThread {
            assertTrue(composeViewAdapter.designInfoList.isNotEmpty())
        }

        assertEquals(1, composeViewAdapter.designInfoList.size)
        assertEquals(expectedResult, composeViewAdapter.designInfoList[0])
    }

    /**
     * Waits for a given condition to be satisfied within a given timeout. Fails the test when
     * timing out. The condition is evaluated on the UI thread.
     */
    private fun waitFor(
        timeout: Long,
        timeUnit: TimeUnit,
        conditionExpression: () -> Boolean
    ) {
        val conditionSatisfied = AtomicBoolean(false)
        val now = System.nanoTime()
        val timeoutNanos = timeUnit.toNanos(timeout)
        while (!conditionSatisfied.get()) {
            activityTestRule.runOnUiThread {
                conditionSatisfied.set(conditionExpression())
            }
            if ((System.nanoTime() - now) > timeoutNanos) {
                // Some previews are expected not to have animations.
                return
            }
            Thread.sleep(200)
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.compose_adapter_test)
            }
        }
    }
}
