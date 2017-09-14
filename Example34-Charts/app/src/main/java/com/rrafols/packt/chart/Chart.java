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
    private Context context;
    private float[] dataPoints;
    private float minValue;
    private float maxValue;
    private float verticalDelta;
    private Paint linePaint;
    private Path graphPath;

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(0xffff0000);
        linePaint.setStrokeWidth(3.f);
        linePaint.setTextSize(50.f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    public void setDataPoints(float[] dataPoints) {
        this.dataPoints = dataPoints;

        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;
        for (int i = 0; i < dataPoints.length; i++) {
            if (dataPoints[i] < minValue) minValue = dataPoints[i];
            if (dataPoints[i] > maxValue) maxValue = dataPoints[i];
        }
        verticalDelta = maxValue - minValue;

        postInvalidate();
    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        animateLogic();
//
//
//        int width = canvas.getWidth() - getPaddingStart() - getPaddingEnd();
//        int height = canvas.getHeight() - getPaddingTop() - getPaddingEnd();
//        canvas.drawText(width + ", " + height, getPaddingStart(), 100 + getPaddingTop(), linePaint);
//
//
//        float lastX = getPaddingStart();
//        float lastY = height * (dataPoints[0] / verticalDelta) + getPaddingTop();
//        for (int i = 1; i < dataPoints.length; i++) {
//            float y = height * (dataPoints[i] / verticalDelta) + getPaddingTop();
//            float x = width * (((float) i) / dataPoints.length) + getPaddingStart();
//
//            canvas.drawLine(lastX, lastY, x, y, linePaint);
//            canvas.drawPoint(lastX, lastY, linePaint);
//            lastX = x;
//            lastY = y;
//        }
//        canvas.drawPoint(lastX, lastY, linePaint);
//
//        if (missingAnimations()) invalidate();
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        animateLogic();


        int width = canvas.getWidth() - getPaddingStart() - getPaddingEnd();
        int height = canvas.getHeight() - getPaddingTop() - getPaddingEnd();
        canvas.drawText(width + ", " + height, getPaddingStart(), 100 + getPaddingTop(), linePaint);

        if (graphPath == null) {
            graphPath = new Path();

            graphPath.moveTo(getPaddingStart(),
                    height * (dataPoints[0] / verticalDelta) + getPaddingTop());

            for (int i = 1; i < dataPoints.length; i++) {
                float y = height * (dataPoints[i] / verticalDelta) + getPaddingTop();
                float x = width * (((float) i) / dataPoints.length) + getPaddingStart();

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
