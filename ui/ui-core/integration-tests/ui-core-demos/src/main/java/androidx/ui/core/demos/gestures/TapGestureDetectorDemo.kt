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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.gesture.tapGestureFilter
import androidx.compose.foundation.Border
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * Simple [tapGestureFilter] demo.
 */
@Composable
fun TapGestureFilterDemo() {
    val color = state { Colors.random() }

    val onTap: (Offset) -> Unit = {
        color.value = color.value.anotherRandomColor()
    }

    Column {
        Text("The box changes color when you tap it.")
        Box(
            Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .preferredSize(192.dp)
                .tapGestureFilter(onTap)
                .clipToBounds(),
            backgroundColor = color.value,
            border = Border(2.dp, BorderColor)
        )
    }
}
