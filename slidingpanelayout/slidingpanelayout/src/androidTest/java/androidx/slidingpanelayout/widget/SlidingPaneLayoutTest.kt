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

package androidx.slidingpanelayout.widget

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.isTwoPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SlidingPaneLayoutTest {

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testLayoutRoot() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun testLayoutWidthSpecExact() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutWidthSpecAtMost() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutWidthSpecUnspecific() {
        TestActivity.onActivityCreated = { activity ->
            val container = LinearLayout(activity)
            val sideButton = Button(activity).apply { text = "button" }
            container.addView(sideButton, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1F))
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                LinearLayout.LayoutParams(0, MATCH_PARENT, 1F)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutHeightSpecExact() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check((ViewAssertions.matches(not(isTwoPane()))))
        }
    }

    @Test
    fun testLayoutHeightSpecAtMost() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(not(isTwoPane())))
        }
    }

    @Test
    fun testLayoutHeightSpecUnspecific() {
        TestActivity.onActivityCreated = { activity ->
            val container = ScrollView(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(not(isTwoPane())))
        }
    }

    @Test
    fun testRemoveDetailView() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                val slidingPaneLayout = findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
                assertThat(slidingPaneLayout.childCount).isEqualTo(2)
                val detailView = findViewById<View>(R.id.detail_pane)
                runOnUiThread {
                    slidingPaneLayout.removeView(detailView)
                }
                assertThat(slidingPaneLayout.childCount).isEqualTo(1)
            }
        }
    }
}
