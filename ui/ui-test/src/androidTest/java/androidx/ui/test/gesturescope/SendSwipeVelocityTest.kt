/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.gesturescope

import androidx.test.filters.MediumTest
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.ui.test.InputDispatcher.InputDispatcherTestRule
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipeWithVelocity
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.SinglePointerInputRecorder
import androidx.ui.test.util.assertOnlyLastEventIsUp
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.downEvents
import androidx.ui.test.util.isAlmostEqualTo
import androidx.ui.test.util.isMonotonicBetween
import androidx.ui.test.util.recordedDuration
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.inMilliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max

/**
 * Tests if we can generate gestures that end with a specific velocity
 */
@MediumTest
@RunWith(Parameterized::class)
class SendSwipeVelocityTest(private val config: TestConfig) {
    data class TestConfig(
        val direction: Direction,
        val duration: Duration,
        val velocity: Float,
        val eventPeriod: Long
    )

    enum class Direction(val from: Offset, val to: Offset) {
        LeftToRight(Offset(boxStart, boxMiddle), Offset(boxEnd, boxMiddle)),
        RightToLeft(Offset(boxEnd, boxMiddle), Offset(boxStart, boxMiddle)),
        TopToBottom(Offset(boxMiddle, boxStart), Offset(boxMiddle, boxEnd)),
        BottomToTop(Offset(boxMiddle, boxEnd), Offset(boxMiddle, boxStart))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (period in listOf(10L, 7L, 16L)) {
                    for (direction in Direction.values()) {
                        for (duration in listOf(100, 500, 1000)) {
                            for (velocity in listOf(79f, 200f, 1500f, 4691f)) {
                                add(
                                    TestConfig(
                                        direction,
                                        Duration(milliseconds = duration.toLong()),
                                        velocity,
                                        period
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        private const val tag = "widget"

        private val boxSize = 500.0f
        private val boxStart = 1.0f
        private val boxMiddle = boxSize / 2
        private val boxEnd = boxSize - 1.0f
    }

    private val start get() = config.direction.from
    private val end get() = config.direction.to
    private val duration get() = config.duration
    private val velocity get() = config.velocity
    private val eventPeriod get() = config.eventPeriod

    private val expectedXVelocity = when (config.direction) {
        Direction.LeftToRight -> velocity
        Direction.RightToLeft -> -velocity
        else -> 0f
    }

    private val expectedYVelocity = when (config.direction) {
        Direction.TopToBottom -> velocity
        Direction.BottomToTop -> -velocity
        else -> 0f
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule(
        disableDispatchInRealTime = true,
        eventPeriodOverride = eventPeriod
    )

    private val recorder = SinglePointerInputRecorder()

    @Test
    fun swipeWithVelocity() {
        composeTestRule.setContent {
            Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                ClickableTestBox(recorder, boxSize, boxSize, tag = tag)
            }
        }

        onNodeWithTag(tag).performGesture {
            swipeWithVelocity(start, end, velocity, duration)
        }

        runOnIdle {
            recorder.run {
                val durationMs = duration.inMilliseconds()
                val minimumEventSize = max(2, (durationMs / eventPeriod).toInt())
                assertThat(events.size).isAtLeast(minimumEventSize)
                assertOnlyLastEventIsUp()

                // Check coordinates
                events.first().position.isAlmostEqualTo(start)
                downEvents.isMonotonicBetween(start, end)
                events.last().position.isAlmostEqualTo(end)

                // Check timestamps
                assertTimestampsAreIncreasing()
                assertThat(recordedDuration).isEqualTo(duration)

                // Check velocity
                val actualVelocity = recordedVelocity.pixelsPerSecond
                assertThat(actualVelocity.x).isWithin(.1f).of(expectedXVelocity)
                assertThat(actualVelocity.y).isWithin(.1f).of(expectedYVelocity)
                assertThat(actualVelocity.getDistance()).isWithin(velocity * 0.001f).of(velocity)
            }
        }
    }
}
