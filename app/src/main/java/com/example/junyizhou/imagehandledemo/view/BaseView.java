package com.example.junyizhou.imagehandledemo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class BaseView extends View {

    public interface OnSizeChangeListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    @IntDef({ActionMode.NONE, ActionMode.DRAG, ActionMode.ZOOM_WITH_TWO_POINTER, ActionMode.ZOOM_WITH_ONE_POINTER})
    @Retention(RetentionPolicy.SOURCE)
    protected @interface ActionMode {
        /**
         * nothing to do
         */
        int NONE = 775;
        /**
         * 单指拖动
         */
        int DRAG = 715;
        /**
         * 两指缩放
         */
        int ZOOM_WITH_TWO_POINTER = 967;
        /**
         * 单指缩放
         */
        int ZOOM_WITH_ONE_POINTER = 866;
    }

    @ActionMode
    protected int mode = ActionMode.NONE;

    protected float anchorX = 0;
    protected float anchorY = 0;
    protected float oldDistance = 1f;
    protected float oldRotation = 0;

    protected PointF midPoint = new PointF();

    protected Matrix moveMatrix = new Matrix();
    protected Matrix downMatrix = new Matrix();
    protected Matrix matrixBig = new Matrix();
    protected Matrix matrixSmall = new Matrix();

    protected RectF targetRect;

    protected boolean isFirst = true;

    protected OnSizeChangeListener mOnSizeChangedListener = null;

    protected Sticker mCropImageGroup = new Sticker();

    protected final Paint mPaintForBitmap;

    public BaseView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mPaintForBitmap = new Paint();
        mPaintForBitmap.setAntiAlias(true);
        mPaintForBitmap.setFilterBitmap(true);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            targetRect = new RectF(left, top, right, bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCropImageGroup.bitmap != null) {
            canvas.drawBitmap(mCropImageGroup.bitmap, mCropImageGroup.entireMatrix, mPaintForBitmap);
        }
    }

    // 触碰两点间距
    public float getDistanceBetweenTwoPoints(MotionEvent event) {
        final float x = event.getX(0) - event.getX(1);
        final float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    // 取手势中心点
    public PointF midPoint(MotionEvent event) {
        PointF point = new PointF();
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);

        return point;
    }

    // 取旋转角
    public float getRotation(MotionEvent event) {
        final double x = event.getX(0) - event.getX(1);
        final double y = event.getY(0) - event.getY(1);
        //atan2(double y,double x) 返回的是原点至点(x,y)的方位角，即与 x 轴的夹角。返回值的单位为弧度，取值范围为
        final double radians = Math.atan2(y, x);
        return (float) Math.toDegrees(radians);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isFirst) {
            isFirst = false;
            setBackgroundBitmap();
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
        setBackgroundBitmap();
    }

    public void setBackgroundBitmap() {
        if (mCropImageGroup.bitmap != null) {
            setBackgroundBitmap(mCropImageGroup.bitmap);
        }
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        mCropImageGroup.bitmap = bitmap;
        if (mCropImageGroup.entireMatrix == null) {
            mCropImageGroup.entireMatrix = new Matrix();
        }
        mCropImageGroup.entireMatrix.reset();

        if (matrixBig != null && matrixSmall != null) {
            matrixBig.reset();
            matrixSmall.reset();
        }

        float scale;
        float transY = (getHeight() - mCropImageGroup.bitmap.getHeight()) / 2;
        float transX = (getWidth() - mCropImageGroup.bitmap.getWidth()) / 2;

        matrixBig.postTranslate(transX, transY);
        if (mCropImageGroup.bitmap.getHeight() <= mCropImageGroup.bitmap.getWidth()) {
            scale = (float) getHeight() / mCropImageGroup.bitmap.getHeight();
        } else {
            scale = (float) getWidth() / mCropImageGroup.bitmap.getWidth();
        }
        matrixBig.postScale(scale, scale, getWidth() / 2, getHeight() / 2);

        matrixSmall.postTranslate(transX, transY);
        if (mCropImageGroup.bitmap.getHeight() >= mCropImageGroup.bitmap.getWidth()) {
            scale = (float) getWidth() / mCropImageGroup.bitmap.getHeight();
        } else {
            scale = (float) getWidth() / mCropImageGroup.bitmap.getWidth();
        }
        matrixSmall.postScale(scale, scale, getWidth() / 2, getHeight() / 2);

        mCropImageGroup.entireMatrix.set(matrixBig);

        invalidate();
    }

    /**
     * 获取贴纸的四个顶点坐标
     *
     * @param imageGroup
     * @return
     */
    protected float[] getBitmapPoints(Sticker imageGroup) {
        return getBitmapPoints(imageGroup.bitmap, imageGroup.entireMatrix);
    }

    /**
     * 得到八个点坐标:按顺序依次是左上，右上，左下，右下
     */
    protected float[] getBitmapPoints(Bitmap bitmap, Matrix matrix) {
        //TODO 可以优化一下，减少创建数组次数
        final float[] dst = new float[8];
        final float[] src = new float[]{
                0, 0,
                bitmap.getWidth(), 0,
                0, bitmap.getHeight(),
                bitmap.getWidth(), bitmap.getHeight()
        };

        matrix.mapPoints(dst, src);
        return dst;
    }

    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        mOnSizeChangedListener = listener;
    }

    protected static class Sticker {
        Bitmap bitmap;
        /**
         * 用来控制整个贴纸的矩阵
         */
        Matrix entireMatrix = new Matrix();
        /**
         * 用来控制贴纸主体的矩阵
         */
        Matrix stickerBodyMatrix = new Matrix();

        Sticker() {
            this(null);
        }

        Sticker(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public void release() {
            if (bitmap != null) {
                bitmap = null;
            }

            if (entireMatrix != null) {
                entireMatrix.reset();
                entireMatrix = null;
            }
            if (stickerBodyMatrix != null) {
                stickerBodyMatrix.reset();
                ;
                stickerBodyMatrix = null;
            }

        }

        public void performFlip(int sx, int sy, float midX, float midY) {
            entireMatrix.postScale(sx, sy, midX, midY);
        }

        public void updateMatrix(Matrix moveMatrix) {
            entireMatrix.set(moveMatrix);
            stickerBodyMatrix.set(moveMatrix);
        }
    }
}
