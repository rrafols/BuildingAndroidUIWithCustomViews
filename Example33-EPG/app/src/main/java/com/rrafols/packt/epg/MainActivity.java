package com.rrafols.packt.epg;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rrafols.packt.epg.data.Channel;
import com.rrafols.packt.epg.data.Program;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EPG epg = (EPG) findViewById(R.id.epg_view);
        populateDummyChannelList(epg);
    }

    private static void populateDummyChannelList(EPG epg) {
        Channel[] channelList = new Channel[20];
        for (int i = 0; i < channelList.length; i++) {
            channelList[i] = new Channel("channel " + i, null);

            Random rnd = new Random();
            long time = System.currentTimeMillis() - 2 * 60 * 60 * 1000;
            for (int j = 0; j < 100; j++ ) {
                long duration = (rnd.nextInt(7) + 1) * 15 * 60 * 1000;
                Program program = new Program("Program " + j,
                        "This will be the description of program " + j,
                        time,time + duration);

                time += duration;
            }
        }

        epg.setChannelList(channelList);
    }
}
