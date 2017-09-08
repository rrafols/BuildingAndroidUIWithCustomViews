package com.rrafols.packt.epg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rrafols.packt.epg.anim.Animator;
import com.rrafols.packt.epg.data.Channel;
import com.rrafols.packt.epg.data.Program;

import java.util.ArrayList;

public class EPG extends View {
    private static final String TAG = EPG.class.getName();
    private static final int CHANNEL_HEIGHT = 60;
    private static final float DEFAULT_TIME_SCALE = 0.05f;
    private static final float PROGRAM_MARGIN = 6;
    private static final int TIME_THRESHOLD = 16;

    private Animator animator;
    private final int channelHeight;
    private final float programMargin;

    private final Paint paintText;
    private final Paint paintProgram;

    private Channel[] channelList;
    private int backgroundColor;

    private float dragX;
    private float dragY;
    private float timeScale;

    private long timeStart;
    private long accTime;

    public EPG(Context context, AttributeSet attrs) {
        super(context, attrs);

        backgroundColor = Color.DKGRAY;
        paintText = new Paint();
        paintText.setAntiAlias(true);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(50.f);

        paintProgram = new Paint();
        paintProgram.setAntiAlias(true);
        paintProgram.setColor(Color.WHITE);
        paintProgram.setStyle(Paint.Style.FILL);

        final float screenDensity = getResources().getDisplayMetrics().density;
        channelHeight = (int) (CHANNEL_HEIGHT * screenDensity + 0.5f);
        timeScale = DEFAULT_TIME_SCALE * screenDensity;
        programMargin = PROGRAM_MARGIN * screenDensity;

        animator = new Animator();
        animator.put("scrollX", 0, 0);
        animator.put("scrollY", 0, 0);

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

        float scrollX = animator.get("scrollX");
        float scrollY = animator.get("scrollY");

        if (scrollX < 0) {
            scrollX = 0;
            animator.put("scrollX", 0, 0);
        }

        if (scrollY < 0) {
            scrollY = 0;
            animator.put("scrollY", 0, 0);
        }

        float maxHeight = channelList.length * channelHeight - getHeight() + 1;
        if (scrollY > maxHeight) {
            scrollY = maxHeight;
            animator.put("scrollY", maxHeight, maxHeight);
        }

        float offset = scrollY;
        int startChannel = (int) (scrollY / channelHeight);
        offset -= startChannel * channelHeight;
        int endChannel = startChannel + getHeight() / channelHeight + 1;
        if (endChannel >= channelList.length) endChannel = channelList.length - 1;


        Log.d(TAG, "Scrolling by: " + scrollY + " : " + startChannel + " : " + endChannel);

        for (int i = startChannel; i <= endChannel; i++) {
            float channelTop = (i - startChannel) * channelHeight - offset;
            float channelBottom = channelTop + channelHeight;

            canvas.drawText("channel " + i, 10, channelHeight/2 + (i - startChannel) * channelHeight - offset, paintText);
            canvas.drawLine(0, channelBottom, getWidth(), channelBottom, paintText);

            canvas.save();
            canvas.clipRect(getWidth() / 4.f, 0, getWidth(), getHeight());
            float horizontalOffset = getWidth() / 4.f - scrollX;
            ArrayList<Program> programs = channelList[i].getPrograms();
            for (int j = 0; j < programs.size(); j++) {
                Program program = programs.get(j);

                long st = program.getStartTime();
                long et = program.getEndTime();
                float dt = (et - st) / 1000.f;

                canvas.drawRoundRect(horizontalOffset + PROGRAM_MARGIN, channelTop + PROGRAM_MARGIN,
                        horizontalOffset + dt * timeScale - PROGRAM_MARGIN, channelBottom - PROGRAM_MARGIN, PROGRAM_MARGIN, PROGRAM_MARGIN, paintProgram);

                horizontalOffset += dt * timeScale;
            }
            canvas.restore();
        }

        if (animator.hasPendingAnimations()) invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

    private void animateLogic() {
        long currentTime = SystemClock.elapsedRealtime();
        accTime += currentTime - timeStart;
        timeStart = currentTime;

        while(accTime > TIME_THRESHOLD) {
            animator.logicTick();
            accTime -= TIME_THRESHOLD;
        }
    }

    private void scrollScreen(float dx, float dy) {
        animator.update("scrollX", dx);
        animator.update("scrollY", dy);
        invalidate();
//
//        scrollX += dx;
//        scrollY += dy;
//
//        if (scrollX < 0) scrollX = 0;
//        if (scrollY < 0) scrollY = 0;
//
//        float maxHeight = channelList.length * channelHeight - getHeight() + 1;
//        if (scrollY > maxHeight) scrollY = maxHeight;
    }
}
