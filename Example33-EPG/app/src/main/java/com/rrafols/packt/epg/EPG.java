package com.rrafols.packt.epg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EPG extends View {
    private static final String TAG = EPG.class.getName();

    private static final int BACKGROUND_COLOR = 0xFF333333;
    private static final int PROGRAM_COLOR = 0xFF666666;
    private static final int HIGHLIGHTED_PROGRAM_COLOR = 0xFFBCBCBC;
    private static final int CURRENT_TIME_COLOR = 0xB4DD1030;

    private static final int CHANNEL_HEIGHT = 80;
    private static final float DEFAULT_TIME_SCALE = 0.0001f;
    private static final float PROGRAM_MARGIN = 4;
    private static final int TIME_THRESHOLD = 16;
    private static final float ANIM_THRESHOLD = 0.01f;
    private static final float TIMEBAR_HEIGHT = 18;

    private final int channelHeight;
    private final float timebarHeight;
    private final float programMargin;

    private final Paint paintTimeBar;
    private final Paint paintChannelText;
    private final Paint paintProgramText;
    private final Paint paintProgram;
    private final Paint paintCurrentTime;

    private Channel[] channelList;
    private int  backgroundColor;

    private float dragX;
    private float dragY;
    private boolean zooming;
    private boolean dragged;
    private float scrollX;
    private float scrollY;
    private float scrollXTarget;
    private float scrollYTarget;
    private float chNameWidthTarget;
    private float chNameWidth;
    private boolean shortChannelMode;
    private boolean switchNameWidth;
    private float timeScale;

    private float frScrollX;
    private float frScrollY;
    private float frChNameWidth;

    private long timeStart;
    private long accTime;
    private Context context;
    private Rect textBoundaries;
    private Rect timeBarTextBoundaries;

    private ScaleGestureDetector scaleDetector;

    private final long initialTimeValue;
    private final Calendar calendar;

    public EPG(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        backgroundColor = BACKGROUND_COLOR;
        paintChannelText = new Paint();
        paintChannelText.setAntiAlias(true);
        paintChannelText.setColor(Color.WHITE);
        paintChannelText.setTextSize(40.f);

        paintProgramText = new Paint();
        paintProgramText.setAntiAlias(true);
        paintProgramText.setColor(Color.BLACK);
        paintProgramText.setTextSize(55.f);

        paintProgram = new Paint();
        paintProgram.setAntiAlias(true);
        paintProgram.setStyle(Paint.Style.FILL);

        paintTimeBar = new Paint();
        paintTimeBar.setTextSize(30.f);
        paintTimeBar.setColor(Color.WHITE);
        paintTimeBar.setAntiAlias(true);

        timeBarTextBoundaries = new Rect();
        paintTimeBar.getTextBounds("88:88", 0, "88:88".length(), timeBarTextBoundaries);

        paintCurrentTime = new Paint();
        paintCurrentTime.setColor(CURRENT_TIME_COLOR);
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
            private long focusTime;
            private float scrollCorrection = 0.f;
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                zooming = true;
                focusTime = getHorizontalPositionTime(scrollXTarget + detector.getFocusX());
                scrollCorrection = getTimeHorizontalPosition((focusTime)) - scrollXTarget;
                return true;
            }

            public boolean onScale(ScaleGestureDetector detector) {
                timeScale *= detector.getScaleFactor();
                timeScale = Math.max(DEFAULT_TIME_SCALE * screenDensity / 2, Math.min(timeScale, DEFAULT_TIME_SCALE * screenDensity * 4));

                float current = getTimeHorizontalPosition((focusTime)) - scrollXTarget;
                float scrollDifference = current - scrollCorrection;
                scrollXTarget += scrollDifference;
                zooming = true;

                invalidate();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                zooming = true;
            }
        });

        chNameWidthTarget = channelHeight;
        chNameWidth = chNameWidthTarget;
        shortChannelMode = true;
        switchNameWidth = false;

        textBoundaries = new Rect();
        timeStart = SystemClock.elapsedRealtime();
        initialTimeValue = System.currentTimeMillis() - 30 * 60 * 1000;
        calendar = Calendar.getInstance();
    }

    public void setChannelList(Channel[] channelList) {
        this.channelList = channelList;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        animateLogic();

        if (switchNameWidth) {
            if (shortChannelMode) {
                chNameWidthTarget = channelHeight * 2;
                shortChannelMode = false;
            } else {
                chNameWidthTarget = channelHeight;
                shortChannelMode = true;
            }
            switchNameWidth = false;
        }

        long currentTime = System.currentTimeMillis();

        drawBackground(canvas);
        drawEPGBody(canvas, currentTime, frScrollY);
        drawTimeBar(canvas, currentTime);
        drawCurrentTime(canvas, currentTime);

        if (missingAnimations()) invalidate();
    }

    private void drawTimeBar(Canvas canvas, long currentTime) {
        calendar.setTimeInMillis(initialTimeValue - 120 * 60 * 1000);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long time = calendar.getTimeInMillis();
        float x = getTimeHorizontalPosition(time) - frScrollX + getWidth() / 4.f;

        while(x < getWidth()) {
            if(x > 0) {
                canvas.drawLine(x, 0, x, timebarHeight, paintTimeBar);
            }

            if(x + timeBarTextBoundaries.width() > 0) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm", Locale.US);
                String date = dateFormatter.format(new Date(time));
                canvas.drawText(date, x + programMargin, (timebarHeight - timeBarTextBoundaries.height()) / 2.f + timeBarTextBoundaries.height(), paintTimeBar);
            }

            time += 30 * 60 * 1000;
            x = getTimeHorizontalPosition(time) - frScrollX + getWidth() / 4.f;
        }

        canvas.drawLine(0, timebarHeight, getWidth(), timebarHeight, paintTimeBar);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawARGB(backgroundColor >> 24, (backgroundColor >> 16) & 0xff,
                (backgroundColor >> 8) & 0xff, backgroundColor & 0xff);
    }

    private void drawCurrentTime(Canvas canvas, long currentTime) {
        float currentTimePos = frChNameWidth + getTimeHorizontalPosition(currentTime) - frScrollX;
        canvas.drawRect(currentTimePos - programMargin/2, 0, currentTimePos + programMargin/2, timebarHeight, paintCurrentTime);
        canvas.clipRect(frChNameWidth, 0, getWidth(), getHeight());
        canvas.drawRect(currentTimePos - programMargin/2, timebarHeight, currentTimePos + programMargin/2, getHeight(), paintCurrentTime);
    }

    private void drawEPGBody(Canvas canvas, long currentTime, float verticalOffset) {
        int startChannel = (int) (frScrollY / channelHeight);
        verticalOffset -= startChannel * channelHeight;
        int endChannel = startChannel + (getHeight() - ((int) (timebarHeight + 0.5f))) / channelHeight + 1;
        if (endChannel >= channelList.length) endChannel = channelList.length - 1;

        canvas.save();
        canvas.clipRect(0, timebarHeight, getWidth(), getHeight());
        for (int i = startChannel; i <= endChannel; i++) {
            float channelTop = (i - startChannel) * channelHeight - verticalOffset + timebarHeight;
            float channelBottom = channelTop + channelHeight;


            if (!shortChannelMode) {
                paintChannelText.getTextBounds(channelList[i].getName(),
                        0,
                        channelList[i].getName().length(),
                        textBoundaries);

                canvas.drawText(channelList[i].getName(),
                        channelHeight - programMargin * 2,
                        (channelHeight - textBoundaries.height()) / 2 + textBoundaries.height() + channelTop,
                        paintChannelText);
            }

            canvas.drawLine(0, channelBottom, getWidth(), channelBottom, paintChannelText);

            if (channelList[i].getIcon() != null) {
                float iconMargin = (channelHeight - channelList[i].getIcon().getHeight()) / 2;
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
            canvas.clipRect(frChNameWidth, 0, getWidth(), getHeight());
            float horizontalOffset = frChNameWidth - frScrollX;
            ArrayList<Program> programs = channelList[i].getPrograms();
            for (int j = 0; j < programs.size(); j++) {
                Program program = programs.get(j);

                long st = program.getStartTime();
                long et = program.getEndTime();

                float programStartX = getTimeHorizontalPosition(st);
                float programEndX = getTimeHorizontalPosition(et);

                if (programStartX - frScrollX > getWidth()) break;

                if (programEndX - frScrollX >= 0) {

                    if (st <= currentTime && et > currentTime) {
                        paintProgram.setColor(HIGHLIGHTED_PROGRAM_COLOR);
                        paintProgramText.setColor(Color.BLACK);
                    } else {
                        paintProgram.setColor(PROGRAM_COLOR);
                        paintProgramText.setColor(Color.WHITE);
                    }

                    canvas.drawRoundRect(horizontalOffset + programMargin + programStartX,
                            channelTop + programMargin,
                            horizontalOffset - programMargin + programEndX,
                            channelBottom - programMargin,
                            programMargin,
                            programMargin,
                            paintProgram);

                    canvas.save();
                    canvas.clipRect(horizontalOffset + programMargin * 2 + programStartX,
                            channelTop + programMargin,
                            horizontalOffset - programMargin * 2 + programEndX,
                            channelBottom - programMargin);

                    paintProgramText.getTextBounds(program.getName(), 0, program.getName().length(), textBoundaries);
                    float textPosition = channelTop + textBoundaries.height() + ((channelHeight - programMargin * 2) - textBoundaries.height()) / 2;
                    canvas.drawText(program.getName(),
                                horizontalOffset + programMargin * 2 + programStartX,
                                textPosition,
                                paintProgramText);
                    canvas.restore();
                }
            }

            canvas.restore();
        }
        canvas.drawLine(frChNameWidth, timebarHeight, frChNameWidth, getHeight(), paintChannelText);
        canvas.restore();
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
                dragged = false;
                return true;

            case MotionEvent.ACTION_UP:
                if (!dragged) {
                    if (event.getX() < frChNameWidth) {
                        switchNameWidth = true;
                        invalidate();
                    }
                }

                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_MOVE:
                float newX = event.getX();
                float newY = event.getY();

                scrollScreen(dragX - newX, dragY - newY);

                dragX = newX;
                dragY = newY;
                dragged = true;
                return true;
            default:
                return false;
        }
    }

    private float getTimeHorizontalPosition(long ts) {
        long timeDifference = (ts - initialTimeValue);
        return timeDifference * timeScale;
    }

    private long getHorizontalPositionTime(float x) {
        return (long) ((x / timeScale) + initialTimeValue);
    }

    private boolean missingAnimations() {
        if (Math.abs(scrollXTarget - scrollX) > ANIM_THRESHOLD) return true;
        if (Math.abs(scrollYTarget - scrollY) > ANIM_THRESHOLD) return true;
        if (Math.abs(chNameWidthTarget - chNameWidth) > ANIM_THRESHOLD) return true;

        return false;
    }

    private void animateLogic() {
        long currentTime = SystemClock.elapsedRealtime();
        accTime += currentTime - timeStart;
        timeStart = currentTime;

        while(accTime > TIME_THRESHOLD) {
            scrollX += (scrollXTarget - scrollX) / 4.f;
            scrollY += (scrollYTarget - scrollY) / 4.f;
            chNameWidth += (chNameWidthTarget - chNameWidth) / 4.f;
            accTime -= TIME_THRESHOLD;
        }

        float factor = ((float) accTime) / TIME_THRESHOLD;
        float nextScrollX = scrollX + (scrollXTarget - scrollX) / 4.f;
        float nextScrollY = scrollY + (scrollYTarget - scrollY) / 4.f;
        float nextChNameWidth = chNameWidth + (chNameWidthTarget - chNameWidth) / 4.f;

        frScrollX = scrollX * (1.f - factor) + nextScrollX * factor;
        frScrollY = scrollY * (1.f - factor) + nextScrollY * factor;
        frChNameWidth = chNameWidth * (1.f - factor) + nextChNameWidth * factor;
    }

    private void scrollScreen(float dx, float dy) {
        scrollXTarget += dx;
        scrollYTarget += dy;

        if (scrollXTarget < -chNameWidth) scrollXTarget = -chNameWidth;
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
