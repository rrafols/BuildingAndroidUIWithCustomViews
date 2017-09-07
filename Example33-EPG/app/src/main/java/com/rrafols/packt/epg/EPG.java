package com.rrafols.packt.epg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rrafols.packt.epg.data.Channel;
import com.rrafols.packt.epg.data.Program;

import java.util.ArrayList;

public class EPG extends View {
    private static final String TAG = EPG.class.getName();
    private static final int CHANNEL_HEIGHT = 50;
    private static final float DEFAULT_TIME_SCALE = 100;

    private final int channelHeight;
    private final Paint paintText;

    private Channel[] channelList;
    private int backgroundColor;
    private float scrollX;
    private float scrollY;

    private float dragX;
    private float dragY;
    private float timeScale;

    public EPG(Context context, AttributeSet attrs) {
        super(context, attrs);

        backgroundColor = Color.DKGRAY;
        paintText = new Paint();
        paintText.setAntiAlias(true);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(50.f);

        scrollX = 0.f;
        scrollY = 0.f;

        final float screenDensity = getResources().getDisplayMetrics().density;
        channelHeight = (int) (CHANNEL_HEIGHT * screenDensity + 0.5f);
        timeScale = DEFAULT_TIME_SCALE * screenDensity;
    }

    public void setChannelList(Channel[] channelList) {
        this.channelList = channelList;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(backgroundColor >> 24, (backgroundColor >> 16) & 0xff,
                (backgroundColor >> 8) & 0xff, backgroundColor & 0xff);

        float offset = scrollY;
        int startChannel = (int) (scrollY / channelHeight);
        offset -= startChannel * channelHeight;
        int endChannel = startChannel + getHeight() / channelHeight + 1;
        if (endChannel >= channelList.length) endChannel = channelList.length - 1;


        Log.d(TAG, "Scrolling by: " + scrollY + " : " + startChannel + " : " + endChannel);

        for (int i = startChannel; i <= endChannel; i++) {
            float channelBottom = (i - startChannel) * channelHeight + channelHeight - offset;
            canvas.drawText("channel " + i, 10 - scrollX, channelHeight/2 + (i - startChannel) * channelHeight - offset, paintText);
            canvas.drawLine(0, channelBottom, getWidth(), channelBottom, paintText);

            ArrayList<Program> programs = channelList[i].getPrograms();
            for (int j = 0; j < programs.size(); j++) {
                Program program = programs.get(j);

                long st = program.getStartTime();
                long et = program.getEndTime();

            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean processed;

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

    private void scrollScreen(float dx, float dy) {
        scrollX += dx;
        scrollY += dy;

        if (scrollX < 0) scrollX = 0;
        if (scrollY < 0) scrollY = 0;

        float maxHeight = channelList.length * channelHeight - getHeight() + 1;
        if (scrollY > maxHeight) scrollY = maxHeight;

        invalidate();
    }
}
