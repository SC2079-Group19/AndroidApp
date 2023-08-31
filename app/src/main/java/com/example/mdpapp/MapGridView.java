package com.example.mdpapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.GridLayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;

public class MapGridView extends GridLayout {
    private Paint paint;
    private Bitmap bitmap;
    private Canvas canvas;

    public MapGridView(Context context) {
        super(context);
        init();
    }

    public MapGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.black));
        paint.setStrokeWidth(5);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(bitmap);
        }
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Draw grid lines
        int rows = getRowCount();
        int cols = getColumnCount();
        int cellWidth = getWidth() / cols;
        int cellHeight = getHeight() / rows;

        paint.setColor(getResources().getColor(android.R.color.darker_gray));
        for (int i = 1; i < cols; i++) {
            int x = cellWidth * i;
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int i = 1; i < rows; i++) {
            int y = cellHeight * i;
            canvas.drawLine(0, y, getWidth(), y, paint);
        }

        // Draw row numbers
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER); // Center-align the text
        for (int i = 1; i < rows; i++) {
            int y = cellHeight * i - cellHeight / 2;
            canvas.drawText(String.valueOf(i), cellWidth / 2, y + paint.getTextSize() / 2, paint);
        }

        // Draw column numbers
        for (int i = 1; i < cols; i++) {
            int x = cellWidth * i - cellWidth / 2;
            canvas.drawText(String.valueOf(i), x, cellHeight / 2 + paint.getTextSize() / 2, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                canvas.drawPoint(x, y, paint);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                canvas.drawLine(x, y, event.getX(), event.getY(), paint);
                invalidate();
                x = event.getX();
                y = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }
}

