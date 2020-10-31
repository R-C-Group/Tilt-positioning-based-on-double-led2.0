package com.example.xianchengqiege;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class MapLayout extends FrameLayout  {

    ImgView map;
    ImageView icon;
    private boolean once=false;
    int newx=0;
    int newy=0;
    int newz=0;
    float width;
    float height;

    float height_v=100000;
    float width_v=100000;



    public MapLayout(@NonNull Context context) {
        super(context);
    }

    public MapLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

    }

    public MapLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        getParent().requestDisallowInterceptTouchEvent(true);
        icon.setX(map.getX()-(-newx*map.scale-width_v/2));
        icon.setY(map.getY()-(-newy*map.scale-width_v/2));
        icon.setZ(map.getZ()-(-newz*map.scale-width_v/2));

        return false;
    }



    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(!once){
            map = new ImgView(getContext());
            map.setImageResource(R.drawable.map1);
            LayoutParams lp1 = new LayoutParams(LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp1.height= (int) height_v;
            lp1.width= (int) width_v;
            lp1.gravity= Gravity.CENTER;
            map.setX(0);
            map.setY(0);
            map.setZ(0);
            map.setScaleType(ImgView.ScaleType.CENTER);
            addView(map, lp1);

            BitmapFactory.Options options = new BitmapFactory.Options();
            BitmapFactory.decodeResource(getResources(), R.drawable.wheel,options);
            height = options.outHeight;
            width = options.outWidth;

            Bitmap bm;

            this.getResources().getDrawable(R.drawable.wheel);

            icon=new ImageView(getContext());
            icon.setImageResource(R.drawable.wheel);
            LayoutParams lp3 = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            icon.setLayoutParams(lp3);
//            icon.setX(width_v/2-width+newx);
//            icon.setY(height_v/2-height+newy);
            icon.setX(0);
            icon.setY(0);
            icon.setZ(0);

            addView(icon);
            once = true;
        }
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }


    public void changeIcon(int x, int y,int z)
    {
        newx=x;
        newy=y;
        newz=z;
        icon.setX(map.getX()-(-newx*map.scale-width_v/2));
        icon.setY(map.getY()-(-newy*map.scale-width_v/2));
        icon.setZ(map.getZ()-(-newz*map.scale-width_v/2));


    }

}
