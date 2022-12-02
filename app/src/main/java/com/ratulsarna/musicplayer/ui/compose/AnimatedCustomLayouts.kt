package com.ratulsarna.musicplayer.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min

@Composable
fun SlidingHeaderLayout(
    bottomSheetProgressFractionProvider: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 1)

        val collapseFraction = bottomSheetProgressFractionProvider()

        val headerWidth = min(
            constraints.maxWidth - CollapsedImageSize.roundToPx(),
            constraints.maxWidth
        )
        val headerHeight = min(
            HeaderHeight.roundToPx(),
            constraints.maxHeight
        )
        val imagePlaceable =
            measurables[0].measure(
                Constraints.fixed(
                    headerWidth,
                    headerHeight
                )
            )

        val headerY = 0
        val headerX = lerp(
            constraints.maxWidth + 100,
            CollapsedImageSize.roundToPx(),
            collapseFraction
        )
        layout(
            width = headerWidth,
            height = headerHeight
        ) {
            imagePlaceable.placeRelative(
                headerX,
                headerY
            )
        }
    }
}

@Composable
fun CollapsingImageLayout(
    modifier: Modifier = Modifier,
    bottomSheetProgressFractionProvider: () -> Float = { 1f },
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 1)

        val collapseFraction = bottomSheetProgressFractionProvider()

        val imageMaxSize =
            min(
                ExpandedImageSize.roundToPx(),
                constraints.maxWidth
            )
        val imageMinSize =
            max(
                CollapsedImageSize.roundToPx(),
                constraints.minWidth
            )
        val imageWidth =
            androidx.compose.ui.util.lerp(
                imageMaxSize,
                imageMinSize,
                collapseFraction
            )
        val imagePlaceable =
            measurables[0].measure(
                Constraints.fixed(
                    imageWidth,
                    imageWidth
                )
            )

        val imageY =
            androidx.compose.ui.unit.lerp(
                constraints.maxHeight.toDp()
                    .div(8f),
                0.dp,
                collapseFraction
            )
                .roundToPx()
        val imageX = androidx.compose.ui.util.lerp(
            (constraints.maxWidth - imageWidth) / 2, // centered when expanded
            0, // right aligned when collapsed
            collapseFraction
        )
        layout(
            width = constraints.maxWidth,
            height = imageY + imageWidth
        ) {
            imagePlaceable.placeRelative(
                imageX,
                imageY
            )
        }
    }
}