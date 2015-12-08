package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.qs.QSPanel.QSTileLayout;
import com.android.systemui.qs.QSPanel.TileRecord;

import java.util.ArrayList;

public class TileLayout extends ViewGroup implements QSTileLayout {

    private static final float TILE_ASPECT = 1.2f;

    private static final String TAG = "TileLayout";

    protected int mColumns;
    private int mCellWidth;
    private int mCellHeight;

    protected final ArrayList<TileRecord> mRecords = new ArrayList<>();

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateResources();
    }

    @Override
    public int getOffsetTop(TileRecord tile) {
        return getTop();
    }

    public void addTile(TileRecord tile) {
        mRecords.add(tile);
        addView(tile.tileView);
    }

    @Override
    public void removeTile(TileRecord tile) {
        mRecords.remove(tile);
        removeView(tile.tileView);
    }

    public void removeAllViews() {
        mRecords.clear();
        super.removeAllViews();
    }

    @Override
    public void setTileVisibility(TileRecord tile, int visibility) {
        tile.tileView.setVisibility(visibility);
    }

    public void updateResources() {
        final Resources res = mContext.getResources();
        final int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        mCellHeight = getCellHeight();
        mCellWidth = (int) (mCellHeight * TILE_ASPECT);
        if (mColumns != columns) {
            mColumns = columns;
            postInvalidate();
        }
    }

    protected int getCellHeight() {
        return mContext.getResources().getDimensionPixelSize(R.dimen.qs_tile_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        int r = -1;
        int c = -1;
        int rows = 0;
        for (TileRecord record : mRecords) {
            if (record.tileView.getVisibility() == GONE) continue;
            // wrap to next column if we've reached the max # of columns
            // also don't allow dual + single tiles on the same row
            if (r == -1 || c == (mColumns - 1)) {
                r++;
                c = 0;
            } else {
                c++;
            }
            record.row = r;
            record.col = c;
            rows = r + 1;
        }

        View previousView = this;
        for (TileRecord record : mRecords) {
            if (record.tileView.getVisibility() == GONE) continue;
            final int cw = mCellWidth;
            final int ch = mCellHeight;
            record.tileView.measure(exactly(cw), exactly(ch));
            previousView = record.tileView.updateAccessibilityOrder(previousView);
        }
        int h = rows == 0 ? 0 : getRowTop(rows);
        setMeasuredDimension(width, h);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getWidth();
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (TileRecord record : mRecords) {
            if (record.tileView.getVisibility() == GONE) continue;
            final int cols = getColumnCount(record.row);
            final int cw = mCellWidth;
            final int extra = (w - cw * cols) / (cols + 1);
            int left = record.col * cw + (record.col + 1) * extra;
            final int top = getRowTop(record.row);
            int right;
            int tileWith = record.tileView.getMeasuredWidth();
            if (isRtl) {
                right = w - left;
                left = right - tileWith;
            } else {
                right = left + tileWith;
            }
            record.tileView.layout(left, top, right, top + record.tileView.getMeasuredHeight());
        }
    }

    private int getRowTop(int row) {
        if (row <= 0) return 0;
        return row * mCellHeight;
    }

    private int getColumnCount(int row) {
        int cols = 0;
        for (TileRecord record : mRecords) {
            if (record.tileView.getVisibility() == GONE) continue;
            if (record.row == row) cols++;
        }
        return cols;
    }

}
