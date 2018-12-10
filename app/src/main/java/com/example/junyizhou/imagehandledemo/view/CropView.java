package com.example.junyizhou.imagehandledemo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CropView extends BaseView {

    public CropView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                anchorX = event.getX();
                anchorY = event.getY();
                downMatrix.set(mCropImageGroup.entireMatrix);
                mode = ActionMode.DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                downMatrix.set(mCropImageGroup.entireMatrix);
                mode = ActionMode.ZOOM_WITH_TWO_POINTER;
                oldDistance = getDistanceBetweenTwoPoints(event);
                midPoint = midPoint(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == ActionMode.ZOOM_WITH_TWO_POINTER) {
                    moveMatrix.set(downMatrix);
                    float newDist = getDistanceBetweenTwoPoints(event);
                    float scale = newDist / oldDistance;
                    moveMatrix.postScale(scale, scale, midPoint.x, midPoint.y);// 縮放
                    mCropImageGroup.entireMatrix.set(moveMatrix);
                    invalidate();

                } else if (mode == ActionMode.DRAG) {
                    moveMatrix.set(downMatrix);
                    moveMatrix.postTranslate(event.getX() - anchorX, event.getY() - anchorY);// 平移
                    mCropImageGroup.entireMatrix.set(moveMatrix);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mCropImageGroup.bitmap != null) {
                    matrixFix();
                }
                mode = ActionMode.NONE;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = ActionMode.NONE;
                break;
        }
        return true;
    }

    private void matrixFix() {
        float[] points = getBitmapPoints(mCropImageGroup.bitmap, moveMatrix);
        float x1 = points[0];
        float y1 = points[1];
        float x2 = points[2];
        float y3 = points[5];

        if (mCropImageGroup.bitmap.getWidth() <= mCropImageGroup.bitmap.getHeight()) {
            if ((x2 - x1) < getWidth()) {
                moveMatrix.set(matrixBig);
            }

            if ((y3 - y1) < getHeight()) {
                moveMatrix.set(matrixSmall);
            }
        } else if (mCropImageGroup.bitmap.getWidth() > mCropImageGroup.bitmap.getHeight()) {
            if ((y3 - y1) < getHeight()) {
                moveMatrix.set(matrixBig);
            }

            if ((x2 - x1) < getWidth()) {
                moveMatrix.set(matrixSmall);
            }
        }

        if (!moveMatrix.equals(matrixBig) && !moveMatrix.equals(matrixSmall)) {
            if (x1 >= targetRect.left) {
                moveMatrix.postTranslate(targetRect.left - x1, 0);
            }

            if (x2 <= targetRect.left + getWidth()) {
                moveMatrix.postTranslate(getWidth() - x2, 0);
            }

            if (y1 >= targetRect.top) {
                moveMatrix.postTranslate(0, targetRect.top - y1);
            }

            if (y3 <= targetRect.top + getHeight()) {
                moveMatrix.postTranslate(0, targetRect.top + getHeight() - y3);
            }
        }

        mCropImageGroup.entireMatrix.set(moveMatrix);
        invalidate();
    }
}