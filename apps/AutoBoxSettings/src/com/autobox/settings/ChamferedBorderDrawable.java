package com.autobox.settings;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Draws the AutoBox cyberpunk preference item border:
 * - Solid fill (white in light mode, near-black in dark mode)
 * - Thin colored border stroke (dark in light mode, white in dark mode)
 * - Diagonal chamfered (cut) corners at all 4 corners
 *
 * Colors are resolved from resources so they automatically adapt to
 * the current light/dark UI mode — no manual theme switching needed.
 *
 * Usage (e.g. in a custom PreferenceGroupAdapter or ItemDecoration):
 *
 *   ChamferedBorderDrawable bg = ChamferedBorderDrawable.fromContext(context);
 *   view.setBackground(bg);
 */
public class ChamferedBorderDrawable extends Drawable {

    private static final float DEFAULT_CORNER_CUT_DP = 8f;
    private static final float DEFAULT_STROKE_DP     = 1.5f;

    private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  shapePath   = new Path();

    private final float cornerCutPx;
    private final float strokeWidthPx;

    private ChamferedBorderDrawable(int fillColor, int borderColor, float density) {
        cornerCutPx   = DEFAULT_CORNER_CUT_DP * density;
        strokeWidthPx = DEFAULT_STROKE_DP     * density;

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        strokePaint.setColor(borderColor);
    }

    /**
     * Creates a drawable with colors resolved from the current theme context.
     * Automatically adapts to light and dark mode.
     */
    public static ChamferedBorderDrawable fromContext(Context context) {
        Resources.Theme theme = context.getTheme();
        int fillColor   = resolveColor(context, R.color.autobox_pref_bg);
        int borderColor = resolveColor(context, R.color.autobox_border);
        float density   = context.getResources().getDisplayMetrics().density;
        return new ChamferedBorderDrawable(fillColor, borderColor, density);
    }

    private static int resolveColor(Context ctx, int resId) {
        return ctx.getResources().getColor(resId, ctx.getTheme());
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        buildPath(bounds);
    }

    private void buildPath(Rect b) {
        float half = strokeWidthPx / 2f;
        float l   = b.left   + half;
        float t   = b.top    + half;
        float r   = b.right  - half;
        float bot = b.bottom - half;
        float c   = cornerCutPx;

        shapePath.reset();
        shapePath.moveTo(l + c, t);         // top-left → after cut
        shapePath.lineTo(r - c, t);         // → top-right before cut
        shapePath.lineTo(r,     t + c);     //   top-right cut
        shapePath.lineTo(r,     bot - c);   // → bottom-right before cut
        shapePath.lineTo(r - c, bot);       //   bottom-right cut
        shapePath.lineTo(l + c, bot);       // → bottom-left before cut
        shapePath.lineTo(l,     bot - c);   //   bottom-left cut
        shapePath.lineTo(l,     t + c);     // → top-left before cut
        shapePath.close();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(shapePath, fillPaint);
        canvas.drawPath(shapePath, strokePaint);
    }

    @Override public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter cf) {
        fillPaint.setColorFilter(cf);
        strokePaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
