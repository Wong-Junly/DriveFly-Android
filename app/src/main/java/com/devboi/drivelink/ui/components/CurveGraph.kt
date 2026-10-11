package com.devboi.drivelink.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.devboi.drivelink.ui.UIConstants
import com.devboi.drivelink.ui.theme.GraphAxesColour
import com.devboi.drivelink.ui.theme.GraphCurveColour
import com.devboi.drivelink.ui.theme.GraphHandleColour
import com.devboi.drivelink.ui.theme.Primary
import com.devboi.drivelink.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun CurveGraph(
    modifier: Modifier,
    onSlope1Change: (Float) -> Unit = {},
    onSlope2Change: (Float) -> Unit = {},
    vm: MainViewModel
){
    val slope1 by vm.steeringCurveSlope1.collectAsState()
    val slope2 by vm.steeringCurveSlope2.collectAsState()

    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    var activeHandle by remember { mutableIntStateOf(0) }

    var handle1 by remember {mutableStateOf(Offset.Zero)}
    var handle2 by remember {mutableStateOf(Offset.Zero)}

    // Local slope variables
    var localSlope1 by remember { mutableFloatStateOf(slope1) }
    var localSlope2 by remember { mutableFloatStateOf(slope2) }

    val handleRadius = with (LocalDensity.current) {
        UIConstants.HandleRadius.toPx()
    }

    // Sync local slopes with vm slopes
    LaunchedEffect(slope1, slope2) {
        localSlope1 = slope1
        localSlope2 = slope2
    }

    // Update handles based on the slopes, only when not dragging them
    LaunchedEffect(canvasSize, slope1, slope2) {
        if (activeHandle == 0) {

            // handle1 at r = 50% of width at angle a, where tan(a) = slope1
            var invLen = 1f / sqrt(1f + slope1 * slope1)
            val r = 0.5f

            val x1Math = r * invLen
            val y1Math = r * slope1 * invLen
            val y1Screen = (1f - y1Math) * canvasSize.height
            handle1 = Offset(x1Math * canvasSize.width, y1Screen)

            // handle2 at r = 50% of width from the top right at angle b, where tan(b) = slope2
            invLen = 1f / sqrt(1f + slope2 * slope2)

            val x2Math = 1f - r * invLen
            val y2Math = 1f - r * slope2 * invLen
            val y2Screen = (1f - y2Math) * canvasSize.height
            handle2 = Offset(x2Math * canvasSize.width, y2Screen)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UIConstants.SpacingMedium)
            .border(1.dp, Primary)
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            activeHandle = when {
                                // Touch radius is 3 times bigger than the visible handle to make it easy
                                // to drag it
                                handle1.isTouched(position, handleRadius * 3f) -> 1
                                handle2.isTouched(position, handleRadius * 3f) -> 2
                                else -> 0
                            }
                        },
                        onDragEnd = {
                            onSlope1Change(localSlope1)
                            onSlope2Change(localSlope2)
                            activeHandle = 0
                                    },
                        onDragCancel = {
                            onSlope1Change(localSlope1)
                            onSlope2Change(localSlope2)
                            activeHandle = 0
                                       },
                        onDrag = { change, _ ->

                            fun clamp(pos: Offset) = Offset(
                                pos.x.coerceIn(5f, size.width.toFloat() - 5f), // 5px margin from the ends to prevent
                                pos.y.coerceIn(0f, size.height.toFloat())      // divide by zero
                            )

                            val newPos = clamp(change.position)

                            when (activeHandle) {
                                // Clamped to prevent overshoot on a cubic hermite curve
                                1 -> {
                                    val x = newPos.x
                                    val minY = size.height * (1f - 3f * x / size.height)

                                    handle1 = newPos.copy(
                                        y = max(newPos.y, minY)
                                    )

                                    // Calculate slope after converting to normalised bottom left origin
                                    // coordinates
                                    localSlope1 = (1f - (handle1.y / size.height)) / (handle1.x / size.width)

                                }

                                2 -> {
                                    val x = newPos.x
                                    val maxY = size.height * (1f - 3f * (x / size.height) + 2f)

                                    handle2 = newPos.copy(
                                        y = min(newPos.y, maxY)
                                    )

                                    // Calculate slope after converting to normalised bottom left origin
                                    // coordinates
                                    localSlope2 = (-handle2.y / size.height) / ((handle2.x / size.width) - 1f)
                                }
                            }
                        }
                    )
                }
        ) {
            canvasSize = size

            // Draw the linear reference line (y=x)
            drawLine(
                color = GraphAxesColour,
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 1.414f
            )

            // Draw the limiting lines. Beyond these there is overshoot on the cubic hermite, so the
            // values are clamped by these for the handles
            drawLine(
                color = GraphAxesColour,
                start = Offset(0f, size.height),
                end = Offset(size.width * 0.3333333f, 0f),
                strokeWidth = 1.414f
            )
            drawLine(
                color = GraphAxesColour,
                start = Offset(size.width * 0.6666666f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 1.414f
            )

            // Background grid
            drawGridLines(10, horizontal = true)
            drawGridLines(10, horizontal = false)

            val path = cubicToBezier(
                0f, 0f,
                1f, 1f,
                localSlope1, localSlope2
            )

            drawPath(path = path, color = GraphCurveColour, style = Stroke(width = 10f))
            drawCircle(
                color = GraphHandleColour,
                radius = handleRadius,
                center = handle1
            )
            drawCircle(
                color = GraphHandleColour,
                radius = handleRadius,
                center = handle2
            )
            drawLine(
                color = GraphHandleColour,
                start = Offset(0f, size.height), end = handle1,
                strokeWidth = 5f
            )
            drawLine(
                color = GraphHandleColour,
                start = Offset(size.width, 0f), end = handle2,
                strokeWidth = 5f
            )
        }
    }
}


// Takes the inputs in 0-1 normalised coordinated in math space (Origin in bottom left)
// and outputs a path in screen space
private fun DrawScope.cubicToBezier(x0: Float, y0: Float, x1: Float, y1: Float, m0: Float, m1: Float): Path {
    // Convert from cubic hermite to an exact Bezier curve, and return the path
    // Reference: https://math.stackexchange.com/questions/4128882/nonparametric-hermite-cubic-to-bezier-curve

    // Calculate bezier control points
    val c1x = x0 + 0.3333333f * (x1 - x0)
    val c1y = y0 + 0.3333333f * m0 * (x1 - x0)

    val c2x = x1 - 0.3333333f * (x1 - x0)
    val c2y = y1 - 0.3333333f * m1 * (x1 - x0)

    // Convert to screen coordinates and draw
    // screenX = x * width
    // screenY = (1 - y) * height
    return Path().apply {
        moveTo(x0, (1f - y0) * size.height);
        cubicTo(
            c1x * size.width,
            (1f - c1y) * size.height,
            c2x * size.width,
            (1f - c2y) * size.height,
            size.width,
            0f
        )
    }

}

private fun DrawScope.drawGridLines(n: Int, horizontal: Boolean) {
    var start: Offset
    var end: Offset

    for (i in 1..<n) {
        if (horizontal) {
            start = Offset(0f, size.width * i / n)
            end = Offset(size.width, size.height * i / n)
        } else {
            start = Offset(size.width * i / n, 0f)
            end = Offset(size.width * i / n, size.height)
        }

        drawLine(
            color = GraphAxesColour,
            start = start,
            end = end,
            strokeWidth = 1f
        )
    }
}

private fun Offset.distanceFrom(point: Offset): Float =
    sqrt((point.x - x)*(point.x - x) + (point.y - y)*(point.y - y))

private fun Offset.isTouched(point: Offset, radius: Float): Boolean =
    (this.distanceFrom(point) < radius)