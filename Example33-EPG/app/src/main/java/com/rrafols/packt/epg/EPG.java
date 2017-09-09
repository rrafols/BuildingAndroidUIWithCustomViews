package com.rrafols.packt.epg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.rrafols.packt.epg.data.Channel;
import com.rrafols.packt.epg.data.Program;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

public class EPG extends View {
    private static final String TAG = EPG.class.getName();
    private static final int CHANNEL_HEIGHT = 80;
    private static final float DEFAULT_TIME_SCALE = 0.08f;
    private static final float PROGRAM_MARGIN = 4;
    private static final int TIME_THRESHOLD = 16;
    private static final float ANIM_THRESHOLD = 0.01f;
    private static final float TIMEBAR_HEIGHT = 18;

    private final int channelHeight;
    private final float timebarHeight;
    private final float programMargin;

    private final Paint paintChannelText;
    private final Paint paintProgramText;
    private final Paint paintProgram;
    private final Paint paintCurrentTime;

    private Channel[] channelList;
    private int backgroundColor;

    private float dragX;
    private float dragY;
    private boolean zooming;
    private float scrollX;
    private float scrollY;
    private float scrollXTarget;
    private float scrollYTarget;
    private float timeScale;

    private long timeStart;
    private long accTime;
    private Context context;
    private Rect textBoundaries;
    private ScaleGestureDetector scaleDetector;

    public EPG(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        backgroundColor = Color.DKGRAY;
        paintChannelText = new Paint();
        paintChannelText.setAntiAlias(true);
        paintChannelText.setColor(Color.WHITE);
        paintChannelText.setTextSize(50.f);

        paintProgramText = new Paint();
        paintProgramText.setAntiAlias(true);
        paintProgramText.setColor(Color.BLACK);
        paintProgramText.setTextSize(55.f);

        paintProgram = new Paint();
        paintProgram.setAntiAlias(true);
        paintProgram.setColor(Color.WHITE);
        paintProgram.setStyle(Paint.Style.FILL);

        paintCurrentTime = new Paint();
        paintCurrentTime.setColor(Color.RED);
        paintCurrentTime.setAlpha(180);
        paintCurrentTime.setStyle(Paint.Style.FILL);


        final float screenDensity = getResources().getDisplayMetrics().density;
        channelHeight = (int) (CHANNEL_HEIGHT * screenDensity + 0.5f);
        timeScale = DEFAULT_TIME_SCALE * screenDensity;
        programMargin = PROGRAM_MARGIN * screenDensity;
        timebarHeight = TIMEBAR_HEIGHT * screenDensity;

        scrollX = 0.f;
        scrollY = 0.f;
        scrollXTarget = 0.f;
        scrollYTarget = 0.f;
        zooming = false;

        // more information:
        // https://developer.android.com/training/gestures/scale.html
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float scrollCorrection = 0.f;
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                scrollCorrection = scrollXTarget * timeScale;
                return true;
            }

            public boolean onScale(ScaleGestureDetector detector) {
                timeScale *= detector.getScaleFactor();
                timeScale = Math.max(DEFAULT_TIME_SCALE * screenDensity / 2, Math.min(timeScale, DEFAULT_TIME_SCALE * screenDensity * 4));
                zooming = true;
//                scrollXTarget += scrollXTarget * timeScale - scrollCorrection;
//                scrollCorrection = scrollXTarget * timeScale;
                invalidate();
                return true;
            }
        });

        textBoundaries = new Rect();
        timeStart = SystemClock.elapsedRealtime();
    }

    public void setChannelList(Channel[] channelList) {
        this.channelList = channelList;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        animateLogic();

        canvas.drawARGB(backgroundColor >> 24, (backgroundColor >> 16) & 0xff,
                (backgroundColor >> 8) & 0xff, backgroundColor & 0xff);

        float offset = scrollY;
        int startChannel = (int) (scrollY / channelHeight);
        offset -= startChannel * channelHeight;
        int endChannel = startChannel + (getHeight() - ((int) (timebarHeight + 0.5f))) / channelHeight + 1;
        if (endChannel >= channelList.length) endChannel = channelList.length - 1;

        canvas.save();
        canvas.clipRect(0, timebarHeight, getWidth(), getHeight());
        for (int i = startChannel; i <= endChannel; i++) {
            float channelTop = (i - startChannel) * channelHeight - offset + timebarHeight;
            float channelBottom = channelTop + channelHeight;

            canvas.drawText("channel " + i, 10, channelHeight/2 + (i - startChannel) * channelHeight - offset, paintChannelText);
            canvas.drawLine(0, channelBottom, getWidth(), channelBottom, paintChannelText);

            if (channelList[i].getIcon() != null) {
                float iconMargin = ((channelBottom - channelTop) - channelList[i].getIcon().getHeight()) / 2;
                canvas.drawBitmap(channelList[i].getIcon(), iconMargin, channelTop + iconMargin, null);
            } else if (!channelList[i].isIconRequested()) {
                channelList[i].setIconRequested(true);

                Picasso.with(context)
                        .load(channelList[i]
                        .getIconUrl())
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into(new ChannelIconTarget(channelList[i]));
            }

            canvas.save();
            canvas.clipRect(getWidth() / 4.f, 0, getWidth(), getHeight());
            float horizontalOffset = getWidth() / 4.f - scrollX;
            ArrayList<Program> programs = channelList[i].getPrograms();
            for (int j = 0; j < programs.size(); j++) {
                Program program = programs.get(j);

                long st = program.getStartTime();
                long et = program.getEndTime();
                float dt = (et - st) / 1000.f;

                canvas.drawRoundRect(horizontalOffset + programMargin, channelTop + programMargin,
                        horizontalOffset + dt * timeScale - programMargin, channelBottom - programMargin, programMargin, programMargin, paintProgram);

                paintProgramText.getTextBounds(program.getName(), 0, program.getName().length(), textBoundaries);
                float textPosition = channelTop + textBoundaries.height() + ((channelHeight - programMargin * 2) - textBoundaries.height()) / 2;
                canvas.drawText(program.getName(), horizontalOffset + programMargin * 2, textPosition, paintProgramText);

                horizontalOffset += dt * timeScale;
            }
            canvas.restore();
        }
        canvas.restore();
        canvas.drawRect(100, 0, 150, getHeight(), paintCurrentTime);

        if (missingAnimations()) invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (zooming) {
            zooming = false;
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragX = event.getX();
                dragY = event.getY();

                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_MOVE:
                float newX = event.getX();
                float newY = event.getY();

                scrollScreen(dragX - newX, dragY - newY);

                dragX = newX;
                dragY = newY;
                return true;
            default:
                return false;
        }
    }

    private boolean missingAnimations() {
        if (Math.abs(scrollXTarget - scrollX) > ANIM_THRESHOLD) return true;
        if (Math.abs(scrollYTarget - scrollY) > ANIM_THRESHOLD) return true;

        return false;
    }

    private void animateLogic() {
        long currentTime = SystemClock.elapsedRealtime();
        accTime += currentTime - timeStart;
        timeStart = currentTime;

        while(accTime > TIME_THRESHOLD) {
            scrollX += (scrollXTarget - scrollX) / 4.f;
            scrollY += (scrollYTarget - scrollY) / 4.f;
            accTime -= TIME_THRESHOLD;
        }
    }

    private void scrollScreen(float dx, float dy) {
        scrollXTarget += dx;
        scrollYTarget += dy;

        if (scrollXTarget < 0) scrollXTarget = 0;
        if (scrollYTarget < 0) scrollYTarget = 0;

        float maxHeight = channelList.length * channelHeight - getHeight() + 1 + timebarHeight;
        if (scrollYTarget > maxHeight) scrollYTarget = maxHeight;

        invalidate();
    }


    class ChannelIconTarget implements Target {
        private Channel ch;

        ChannelIconTarget(Channel ch) {
            this.ch = ch;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            ch.setIcon(bitmap);
            invalidate();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {}

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {}
    }
}
