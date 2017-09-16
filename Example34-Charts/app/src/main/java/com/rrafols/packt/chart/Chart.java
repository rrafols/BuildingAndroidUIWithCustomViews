package com.rrafols.packt.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class Chart extends View {
    private static final String TAG = Chart.class.getName();
    private static final int TIME_THRESHOLD = 16;
    private static final float ANIM_THRESHOLD = 0.01f;

    private float scrollX;
    private float scrollY;
    private float scrollXTarget;
    private float scrollYTarget;
    private float frScrollX;
    private float frScrollY;

    private long timeStart;
    private long accTime;
    private float[] dataPoints;
    private Paint linePaint;
    private Paint circlePaint;
    private Path graphPath;

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(0xffffffff);
        linePaint.setStrokeWidth(8.f);
        linePaint.setStyle(Paint.Style.STROKE);

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(0xffffffff);
        circlePaint.setStyle(Paint.Style.FILL);
    }

    // make a copy of the data as the original array content might change.
    public void setDataPoints(float[] originalData) {
        dataPoints = new float[originalData.length];

        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;
        for (int i = 0; i < dataPoints.length; i++) {
            dataPoints[i] = originalData[i];
            if (dataPoints[i] < minValue) minValue = dataPoints[i];
            if (dataPoints[i] > maxValue) maxValue = dataPoints[i];
        }
        float verticalDelta = maxValue - minValue;

        for (int i = 0; i < dataPoints.length; i++) {
            dataPoints[i] = (dataPoints[i] - minValue) / verticalDelta;
        }

        postInvalidate();
    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        canvas.drawARGB(255,0 ,0 ,0);
//        if (LAYOUT_DIRECTION_LTR == getLayoutDirection()) {
//
//        } else {
//
//        }
//        animateLogic();
//
//        int leftPadding = getPaddingLeft();
//        int topPadding = getPaddingTop();
//
//        int width = canvas.getWidth() - leftPadding - getPaddingEnd();
//        int height = canvas.getHeight() - topPadding - getPaddingBottom();
//
//        float lastX = getPaddingStart();
//        float lastY = height * dataPoints[0] + topPadding;
//        for (int i = 1; i < dataPoints.length; i++) {
//            float y = height * dataPoints[i] + topPadding;
//            float x = width * (((float) i) / dataPoints.length) + leftPadding;
//
//            canvas.drawLine(lastX, lastY, x, y, linePaint);
//            canvas.drawCircle(lastX, lastY, 10, circlePaint);
//            lastX = x;
//            lastY = y;
//        }
//        canvas.drawCircle(lastX, lastY, 10, circlePaint);
//
//        if (missingAnimations()) invalidate();
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        animateLogic();

        canvas.drawARGB(255,0 ,0 ,0);

        int leftPadding = getPaddingLeft();
        int topPadding = getPaddingTop();

        int width = canvas.getWidth() - leftPadding - getPaddingEnd();
        int height = canvas.getHeight() - topPadding - getPaddingBottom();

        if (graphPath == null) {
            graphPath = new Path();

            graphPath.moveTo(leftPadding,height * dataPoints[0] + topPadding);

            for (int i = 1; i < dataPoints.length; i++) {
                float y = height * dataPoints[i] + topPadding;
                float x = width * (((float) i + 1) / dataPoints.length) + leftPadding;

                graphPath.lineTo(x, y);
            }
        }

        canvas.drawPath(graphPath, linePaint);

        if (missingAnimations()) invalidate();
    }

    /**
     * Check if there is any animation that has not finished.
     */
    private boolean missingAnimations() {
        if (Math.abs(scrollXTarget - scrollX) > ANIM_THRESHOLD) return true;
        if (Math.abs(scrollYTarget - scrollY) > ANIM_THRESHOLD) return true;

        return false;
    }

    /**
     * Execute logic iterations and interpolate between current and next logic iteration
     */
    private void animateLogic() {
        long currentTime = SystemClock.elapsedRealtime();
        accTime += currentTime - timeStart;
        timeStart = currentTime;

        while (accTime > TIME_THRESHOLD) {
            scrollX += (scrollXTarget - scrollX) / 4.f;
            scrollY += (scrollYTarget - scrollY) / 4.f;
            accTime -= TIME_THRESHOLD;
        }

        float factor = ((float) accTime) / TIME_THRESHOLD;
        float nextScrollX = scrollX + (scrollXTarget - scrollX) / 4.f;
        float nextScrollY = scrollY + (scrollYTarget - scrollY) / 4.f;

        frScrollX = scrollX * (1.f - factor) + nextScrollX * factor;
        frScrollY = scrollY * (1.f - factor) + nextScrollY * factor;
    }

    /**
     * scroll screen by dx, dy and trigger a redraw cycle.
     */
    private void scrollScreen(float dx, float dy) {
        scrollXTarget += dx;
        scrollYTarget += dy;

        if (scrollXTarget < 0) scrollXTarget = 0;
        if (scrollYTarget < 0) scrollYTarget = 0;

        invalidate();
    }
}
