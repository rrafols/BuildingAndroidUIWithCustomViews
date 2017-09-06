package com.rrafols.packt.epg;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.rrafols.packt.epg.data.Channel;

public class EPG extends View {

    private Channel[] channelList;

    public EPG(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setChannelList(Channel[] channelList) {
        this.channelList = channelList;
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }
}
