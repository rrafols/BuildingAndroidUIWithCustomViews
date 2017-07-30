package com.packt.rrafols.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FixedTimestepExample extends View {
    private static final int BACKGROUND_COLOR = 0xff404060;
    private static final int FOREGROUND_COLOR = 0xffffffff;
    private static final int N_PARTICLES = 800;
    private static final int PARTICLE_SIZE = 4;
    private static final int TIME_THRESHOLD = 16;
    private static final int SPAWN_RATE = 8;

    private Paint paint;
    private Particle[] particles;
    private int particleIndex;
    private long timeStart;
    private long accTime;
    private int previousVisibility;
    private long invisibleTimeStart;


    public FixedTimestepExample(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(FOREGROUND_COLOR);

        particles = new Particle[N_PARTICLES];
        for (int i = 0; i < N_PARTICLES; i++) {
            particles[i] = new Particle();
        }

        particleIndex = 0;
        timeStart = -1;
        accTime = 0;
        previousVisibility = View.GONE;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        // avoid doing this check before View is even visible
        if (timeStart != -1) {
            if ((visibility == View.INVISIBLE || visibility == View.GONE) &&
                    previousVisibility == View.VISIBLE) {

                invisibleTimeStart = SystemClock.elapsedRealtime();
            }

            if ((previousVisibility == View.INVISIBLE || previousVisibility == View.GONE) &&
                    visibility == View.VISIBLE) {

                timeStart += SystemClock.elapsedRealtime() - invisibleTimeStart;
            }
        } else {
            timeStart = SystemClock.elapsedRealtime();
        }

        previousVisibility = visibility;
    }

    private void spawnParticle(float x, float y) {
        for (int i = 0; i < SPAWN_RATE; i++) {
            particles[particleIndex].x = x;
            particles[particleIndex].y = y;
            particles[particleIndex].vx = (float) (Math.random() * 40.f) - 20.f;
            particles[particleIndex].vy = (float) (Math.random() * 20.f) - 10.f;
            particles[particleIndex].ttl = (float) (Math.random() * 100.f) + 150.f;

            particleIndex++;
            if (particleIndex == N_PARTICLES) particleIndex = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                spawnParticle(event.getX(), event.getY());
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void animateParticles(int width, int height) {
        long currentTime = SystemClock.elapsedRealtime();
        accTime += currentTime - timeStart;
        timeStart = currentTime;

        while(accTime > TIME_THRESHOLD) {
            for (int i = 0; i < N_PARTICLES; i++) {
                particles[i].logicTick(width, height);
            }

            accTime -= TIME_THRESHOLD;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        animateParticles(getWidth(), getHeight());

        canvas.drawColor(BACKGROUND_COLOR);

        for(int i = 0; i < N_PARTICLES; i++) {
            float px = particles[i].x;
            float py = particles[i].y;
            float ttl = particles[i].ttl;

            if(ttl > 0) {
                canvas.drawRect(
                        px - PARTICLE_SIZE,
                        py - PARTICLE_SIZE,
                        px + PARTICLE_SIZE,
                        py + PARTICLE_SIZE, paint);
            }
        }
        postInvalidateDelayed(10);
    }

    class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float ttl;

        Particle() {
            ttl = 0.f;
        }

        void logicTick(int width, int height) {
            ttl--;

            if (ttl > 0) {
                vx = vx * 0.95f;
                vy = vy + 0.2f;

                x += vx;
                y += vy;

                if (y < 0) {
                    y = 0;
                    vy = -vy * 0.8f;
                }

                if (x < 0) {
                    x = 0;
                    vx = -vx * 0.8f;
                }

                if (x >= width) {
                    x = width - 1;
                    vx = -vx * 0.8f;
                }
            }
        }
    }
}