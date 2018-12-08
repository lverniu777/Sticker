package com.example.junyizhou.imagehandledemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.example.junyizhou.imagehandledemo.R;

import java.util.ArrayList;
import java.util.List;

public class StickerView extends BaseView {

    /**
     * 添加进来的贴纸容器
     */
    private final List<Sticker> mStickerList = new ArrayList<>();
    /**
     * 画贴纸的边框和删除按钮的圆形框
     */
    private final Paint mPaintForLineAndCircle = new Paint(Paint.ANTI_ALIAS_FLAG);

    {
        mPaintForLineAndCircle.setAntiAlias(true);
        mPaintForLineAndCircle.setColor(Color.BLACK);
        mPaintForLineAndCircle.setAlpha(170);
    }

    /**
     * 当前选中或者是作用的贴纸索引
     */
    private int mCurrentStickerIndex = 0;
    /**
     * 当前删除的贴纸索引
     */
    private int mCurrentDeleteIndex = 0;
    /**
     * 删除小叉子按钮
     */
    private Bitmap mDeleteIcon;


    public StickerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mDeleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_close);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //后添加的先画，保证层级的正确性
        for (int i = mStickerList.size() - 1; i >= 0; i--) {
            final Sticker sticker = mStickerList.get(i);
            if (sticker == null) {
                continue;
            }
            if (sticker.bitmap == null) {
                throw new NullPointerException("bitmap is null");
            }
            if (sticker.bitmap.isRecycled()) {
                throw new IllegalStateException("bitmap is invalid");
            }
            final float[] points = getBitmapPoints(sticker);
            final float x1 = points[0];
            final float y1 = points[1];
            final float x2 = points[2];
            final float y2 = points[3];
            final float x3 = points[4];
            final float y3 = points[5];
            final float x4 = points[6];
            final float y4 = points[7];
            //当前绘制的贴纸是不是获取了焦点
            final boolean isCurrentFocusSticker = i == mCurrentStickerIndex;
            drawBounds(canvas, x1, y1, x2, y2, x3, y3, x4, y4, isCurrentFocusSticker);
            drawDeleteBtn(canvas, x2, y2, isCurrentFocusSticker);
            drawStickerBitmap(canvas, sticker);
        }
    }

    private void drawBounds(Canvas canvas, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, boolean hasFocused) {
        if (!hasFocused) {
            return;
        }
        canvas.drawLine(x1, y1, x2, y2, mPaintForLineAndCircle);
        canvas.drawLine(x2, y2, x4, y4, mPaintForLineAndCircle);
        canvas.drawLine(x4, y4, x3, y3, mPaintForLineAndCircle);
        canvas.drawLine(x3, y3, x1, y1, mPaintForLineAndCircle);
    }

    private void drawDeleteBtn(Canvas canvas, float x2, float y2, boolean hasFocused) {
        if (!hasFocused) {
            return;
        }
        canvas.drawCircle(x2, y2, 40, mPaintForLineAndCircle);
        canvas.drawBitmap(mDeleteIcon, x2 - mDeleteIcon.getWidth() / 2, y2 - mDeleteIcon.getHeight() / 2, mPaintForBitmap);
    }

    private void drawStickerBitmap(Canvas canvas, Sticker sticker) {
        canvas.drawBitmap(sticker.bitmap, sticker.matrix, mPaintForBitmap);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                anchorX = event.getX();
                anchorY = event.getY();
                updateFocusedStickerIndex(stickerCheck(anchorX, anchorY));
                mCurrentDeleteIndex = deleteCheck(anchorX, anchorY);
                if (mCurrentStickerIndex != -1 && mCurrentDeleteIndex == -1) {
                    downMatrix.set(mStickerList.get(mCurrentStickerIndex).matrix);
                    mode = DRAG;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mCurrentStickerIndex = stickerCheck(event.getX(0), event.getY(0));
                final int transformTag = stickerCheck(event.getX(1), event.getY(1));
                if (mCurrentStickerIndex != -1 && transformTag == mCurrentStickerIndex && mCurrentDeleteIndex == -1) {
                    downMatrix.set(mStickerList.get(mCurrentStickerIndex).matrix);
                    mode = ZOOM;
                }
                oldDistance = getDistance(event);
                oldRotation = getRotation(event);
                midPoint = midPoint(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mCurrentStickerIndex <= -1 || mCurrentStickerIndex >= mStickerList.size()) {
                    return true;
                }
                final Sticker sticker = mStickerList.get(mCurrentStickerIndex);
                if (mode == ZOOM) {
                    moveMatrix.set(downMatrix);
                    float newRotation = getRotation(event) - oldRotation;
                    float newDistance = getDistance(event);
                    float scale = newDistance / oldDistance;
                    //缩放
                    moveMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
                    //旋转
                    moveMatrix.postRotate(newRotation, midPoint.x, midPoint.y);
                    sticker.matrix.set(moveMatrix);
                    invalidate();
                } else if (mode == DRAG) {
                    moveMatrix.set(downMatrix);
                    //进行平移操作
                    moveMatrix.postTranslate(event.getX() - anchorX, event.getY() - anchorY);
                    mStickerList.get(mCurrentStickerIndex).matrix.set(moveMatrix);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mCurrentDeleteIndex <= -1 || mCurrentDeleteIndex >= mStickerList.size()) {
                    return true;
                }
                final Sticker stickerToBeRemoved;
                if ((stickerToBeRemoved = mStickerList.remove(mCurrentDeleteIndex)) != null) {
                    stickerToBeRemoved.release();
                }
                invalidate();
                mode = NONE;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        return true;
    }

    private boolean pointCheck(Sticker imageGroup, float x, float y) {
        float[] points = getBitmapPoints(imageGroup);
        float x1 = points[0];
        float y1 = points[1];
        float x2 = points[2];
        float y2 = points[3];
        float x3 = points[4];
        float y3 = points[5];
        float x4 = points[6];
        float y4 = points[7];

        float edge = (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        if ((2 + Math.sqrt(2)) * edge >= Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2))
                + Math.sqrt(Math.pow(x - x2, 2) + Math.pow(y - y2, 2))
                + Math.sqrt(Math.pow(x - x3, 2) + Math.pow(y - y3, 2))
                + Math.sqrt(Math.pow(x - x4, 2) + Math.pow(y - y4, 2))) {
            return true;
        }
        return false;
    }

    private boolean circleCheck(Sticker imageGroup, float x, float y) {
        float[] points = getBitmapPoints(imageGroup);
        float x2 = points[2];
        float y2 = points[3];

        int checkDis = (int) Math.sqrt(Math.pow(x - x2, 2) + Math.pow(y - y2, 2));

        if (checkDis < 40) {
            return true;
        }
        return false;
    }

    private int deleteCheck(float x, float y) {
        for (int i = 0; i < mStickerList.size(); i++) {
            if (circleCheck(mStickerList.get(i), x, y)) {
                return i;
            }
        }
        return -1;
    }

    private int stickerCheck(float x, float y) {
        for (int i = 0; i < mStickerList.size(); i++) {
            if (pointCheck(mStickerList.get(i), x, y)) {
                return i;
            }
        }
        return -1;
    }

    public void addDecal(Bitmap bitmap) {
        if (bitmap == null) {
            throw new NullPointerException("bitmap is null");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalStateException("bitmap has bean recycled");
        }
        final Sticker imageGroupTemp = new Sticker(bitmap);
        if (imageGroupTemp.matrix == null) {
            imageGroupTemp.matrix = new Matrix();
        }
        float transX = (getWidth() - imageGroupTemp.bitmap.getWidth()) / 2;
        float transY = (getHeight() - imageGroupTemp.bitmap.getHeight()) / 2;
        imageGroupTemp.matrix.postTranslate(transX, transY);
        imageGroupTemp.matrix.postScale(0.5f, 0.5f, getWidth() / 2, getHeight() / 2);
        mStickerList.add(imageGroupTemp);
        //最新添加的贴纸即为当前获得焦点的贴纸
        updateFocusedStickerIndex(mStickerList.size() - 1);
        invalidate();
    }

    private void updateFocusedStickerIndex(int i) {
        mCurrentStickerIndex = i;
    }
}