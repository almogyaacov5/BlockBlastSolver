package com.example.blockblastsolver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SolutionBoardAdapter extends BaseAdapter {

    private final Context context;
    private final int[] boardState;

    public SolutionBoardAdapter(Context context, int[] boardState) {
        this.context = context;
        this.boardState = boardState;
    }

    @Override public int getCount() { return 64; }
    @Override public Object getItem(int p) { return boardState[p]; }
    @Override public long getItemId(int p) { return p; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CellView cell;
        if (convertView instanceof CellView) {
            cell = (CellView) convertView;
        } else {
            int displayWidth = context.getResources().getDisplayMetrics().widthPixels;
            int cellSize = (displayWidth - 32) / 8;
            cell = new CellView(context, cellSize);
        }
        cell.setState(boardState[position]);
        return cell;
    }

    // תא מותאם אישית שמצייר את עצמו עם מסגרת
    private static class CellView extends View {
        private final Paint fillPaint = new Paint();
        private final Paint borderPaint = new Paint();
        private int state = 0;
        private final int size;

        public CellView(Context context, int size) {
            super(context);
            this.size = size;

            borderPaint.setColor(Color.parseColor("#555555")); // צבע קו הרשת
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f); // עובי קו הרשת

            fillPaint.setStyle(Paint.Style.FILL);
        }

        public void setState(int state) {
            this.state = state;
            invalidate(); // מצייר מחדש
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(size, size); // תא ריבועי
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // בחירת צבע מילוי לפי מצב התא
            switch (state) {
                case 1:
                    fillPaint.setColor(Color.parseColor("#FF9800")); // כתום - בלוק קיים
                    break;
                case 2:
                    fillPaint.setColor(Color.parseColor("#4CAF50")); // ירוק - בלוק חדש
                    break;
                case 3:
                    fillPaint.setColor(Color.parseColor("#2196F3")); // כחול - שורה/עמודה נמחקה
                    break;
                default:
                    fillPaint.setColor(Color.parseColor("#212121")); // שחור כהה - ריק
                    break;
            }

            float w = getWidth();
            float h = getHeight();
            float padding = 2f;

            // ציור פנים התא
            canvas.drawRect(padding, padding, w - padding, h - padding, fillPaint);

            // ציור מסגרת (קווי רשת - אורך ורוחב)
            canvas.drawRect(padding, padding, w - padding, h - padding, borderPaint);
        }
    }
}
