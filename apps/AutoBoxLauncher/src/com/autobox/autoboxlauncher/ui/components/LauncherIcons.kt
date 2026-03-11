package com.autobox.autoboxlauncher.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LauncherIcons {
    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 3f)
            lineTo(4f, 9f)
            verticalLineTo(20f)
            curveTo(4f, 20.55f, 4.45f, 21f, 5f, 21f)
            horizontalLineTo(9f)
            verticalLineTo(15f)
            horizontalLineTo(15f)
            verticalLineTo(21f)
            horizontalLineTo(19f)
            curveTo(19.55f, 21f, 20f, 20.55f, 20f, 20f)
            verticalLineTo(9f)
            lineTo(12f, 3f)
            close()
        }.build()

    val Navigation: ImageVector
        get() = ImageVector.Builder(
            name = "Navigation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            lineTo(19f, 21f)
            lineTo(12f, 17f)
            lineTo(5f, 21f)
            lineTo(12f, 2f)
            close()
        }.build()

    val Music: ImageVector
        get() = ImageVector.Builder(
            name = "Music",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(12f, 3f)
            verticalLineTo(13.55f)
            curveTo(11.41f, 13.21f, 10.73f, 13f, 10f, 13f)
            curveTo(7.79f, 13f, 6f, 14.79f, 6f, 17f)
            curveTo(6f, 19.21f, 7.79f, 21f, 10f, 21f)
            curveTo(12.21f, 21f, 14f, 19.21f, 14f, 17f)
            verticalLineTo(7f)
            horizontalLineTo(18f)
            verticalLineTo(3f)
            horizontalLineTo(12f)
            close()
        }.build()

    val Phone: ImageVector
        get() = ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(6.62f, 10.79f)
            curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
            lineTo(15.41f, 15.18f)
            curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.94f)
            curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
            curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
            verticalLineTo(19.51f)
            curveTo(21f, 20.06f, 20.55f, 20.51f, 20f, 20.51f)
            curveTo(10.61f, 20.51f, 3f, 12.9f, 3f, 3.51f)
            curveTo(3f, 2.96f, 3.45f, 2.51f, 4f, 2.51f)
            horizontalLineTo(7f)
            curveTo(7.55f, 2.51f, 8f, 2.96f, 8f, 3.51f)
            curveTo(8f, 4.75f, 8.2f, 5.96f, 8.57f, 7.08f)
            curveTo(8.69f, 7.43f, 8.6f, 7.83f, 8.33f, 8.1f)
            lineTo(6.62f, 10.79f)
            close()
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f
        ) {
            moveTo(12f, 12f)
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            close()
            moveTo(19.43f, 12.98f)
            curveTo(19.47f, 12.66f, 19.5f, 12.34f, 19.5f, 12f)
            curveTo(19.5f, 11.66f, 19.47f, 11.34f, 19.43f, 11.02f)
            lineTo(21.54f, 9.37f)
            curveTo(21.73f, 9.22f, 21.78f, 8.95f, 21.66f, 8.73f)
            lineTo(19.66f, 5.27f)
            curveTo(19.54f, 5.05f, 19.27f, 4.97f, 19.05f, 5.05f)
            lineTo(16.56f, 6.05f)
            curveTo(16.04f, 5.65f, 15.48f, 5.32f, 14.87f, 5.07f)
            lineTo(14.49f, 2.42f)
            curveTo(14.46f, 2.18f, 14.25f, 2f, 14f, 2f)
            horizontalLineTo(10f)
            curveTo(9.75f, 2f, 9.54f, 2.18f, 9.51f, 2.42f)
            lineTo(9.13f, 5.07f)
            curveTo(8.52f, 5.32f, 7.96f, 5.66f, 7.44f, 6.05f)
            lineTo(4.95f, 5.05f)
            curveTo(4.73f, 4.97f, 4.46f, 5.05f, 4.34f, 5.27f)
            lineTo(2.34f, 8.73f)
            curveTo(2.22f, 8.95f, 2.27f, 9.22f, 2.46f, 9.37f)
            lineTo(4.57f, 11.02f)
            curveTo(4.53f, 11.34f, 4.5f, 11.66f, 4.5f, 12f)
            curveTo(4.5f, 12.34f, 4.53f, 12.66f, 4.57f, 12.98f)
            lineTo(2.46f, 14.63f)
            curveTo(2.27f, 14.78f, 2.22f, 15.05f, 2.34f, 15.27f)
            lineTo(4.34f, 18.73f)
            curveTo(4.46f, 18.95f, 4.73f, 19.03f, 4.95f, 18.95f)
            lineTo(7.44f, 17.95f)
            curveTo(7.96f, 18.35f, 8.52f, 18.68f, 9.13f, 18.93f)
            lineTo(9.51f, 21.58f)
            curveTo(9.54f, 21.82f, 9.75f, 22f, 10f, 22f)
            horizontalLineTo(14f)
            curveTo(14.25f, 22f, 14.46f, 21.82f, 14.49f, 21.58f)
            lineTo(14.87f, 18.93f)
            curveTo(15.48f, 18.68f, 16.04f, 18.34f, 16.56f, 17.95f)
            lineTo(19.05f, 18.95f)
            curveTo(19.27f, 19.03f, 19.54f, 18.95f, 19.66f, 18.73f)
            lineTo(21.66f, 15.27f)
            curveTo(21.78f, 15.05f, 21.73f, 14.78f, 21.54f, 14.63f)
            lineTo(19.43f, 12.98f)
            close()
        }.build()
}
