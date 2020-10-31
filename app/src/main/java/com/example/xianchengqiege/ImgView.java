package com.example.xianchengqiege;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ImgView extends androidx.appcompat.widget.AppCompatImageView {


    // 属性变量
    private float translationX; // 移动X
    private float translationY; // 移动Y
    public float scale=1; // 伸缩比例
    public float rotation; // 旋转角度

    // 移动过程中临时变量
    private float actionX;
    private float actionY;
    private float spacing;
    private float degree;
    private int moveType; // 0=未选择，1=拖动，2=缩放
    private int count = 0;//双击事件
    private long firstClick = 0;//第一次点击时间
    private long secondClick = 0;//第二次点击时间
    private final int totalTime = 400;
    private boolean once = false;


    public ImgView(Context context) {
        super(context);
    }

    public ImgView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImgView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                moveType = 1;
                actionX = event.getRawX();
                actionY = event.getRawY();
                count++;
                if (1 == count) {
                    firstClick = System.currentTimeMillis();//记录第一次点击时间
                } else if (2 == count) {
                    secondClick = System.currentTimeMillis();//记录第二次点击时间
                    if (secondClick - firstClick < totalTime) {//判断二次点击时间间隔是否在设定的间隔时间之内
                        rotation=0;
                        setRotation(0);
                        setRotation(0);
                        scale = 1;
                        setScaleX(1);
                        setScaleY(1);

                        count = 0;
                        firstClick = 0;
                    } else {
                        firstClick = secondClick;
                        count = 1;
                    }
                    secondClick = 0;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                moveType = 2;
                spacing = getSpacing(event);
                degree = getDegree(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (moveType == 1) {
                    translationX = translationX + event.getRawX() - actionX;
                    translationY = translationY + event.getRawY() - actionY;
                    setTranslationX(translationX);
                    setTranslationY(translationY);
                    actionX = event.getRawX();
                    actionY = event.getRawY();
                }
              else if (moveType == 2) {
                    scale = scale * getSpacing(event) / spacing;
                    setScaleX(scale);
                    setScaleY(scale);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                moveType = 0;
        }
        return true;

    }

    // 触碰两点间距离
    private float getSpacing(MotionEvent event) {
        //通过三角函数得到两点间的距离
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取旋转角度
    private float getDegree(MotionEvent event) {
        //得到两个手指间的旋转角度
        double delta_x = event.getX(0) - event.getX(1);
        double delta_y = event.getY(0) - event.getY(1);
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }




}
