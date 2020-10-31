package com.example.xianchengqiege;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.morphologyEx;


public class Photo_process {





    public double nx = 1;
    public double ny = 1;
    public double nz = 1;
    double l[] = {0, 0, 0};
    double width1=0;
    double height1=0;



    private double r = 5.25;
    public Bitmap selectbp;


    private LED_ROI LED_ROI1,LED_ROI2;
    private LED_ROI LEDone,LEDtwo;



    public Photo_process(Bitmap bitmap, float[] prefValues) {
        selectbp = bitmap;
        FeaturePoint(selectbp,prefValues);
    }


    private void FeaturePoint(Bitmap bmp,float[] prefValues) {
        Point center;
        Mat gray_img = new Mat(), bin_img = new Mat();
        Mat src = new Mat();
        Mat closed = new Mat();
//        Mat element;
        Mat blurred = new Mat();
        Mat edges = new Mat();


        bitmapToMat(bmp, src);



        try {
            //图像处理
            Mat temp=new Mat();
            Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
            Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);//灰度化处理。
            Imgproc.threshold(temp, bin_img, 0, 255, Imgproc.THRESH_OTSU);//二值化

            //bin_img存放二值化的图片
            src = bin_img;//为了获取图片的尺寸
            Mat dst = new Mat();
            ////先膨胀后腐蚀,闭运算
            Mat element = getStructuringElement(MORPH_ELLIPSE, new Size(20, 20));//定义结构元素,size要比单灯的大，才效果好
            morphologyEx(bin_img, dst, MORPH_CLOSE, element);


            //###################################闭运算#################################


            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$切割LED$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            List<Coordinate> LED = new ArrayList<Coordinate>();
            Mat abc = dst.clone();

            for (int ii = 1; ii < 3; ii++) {
                src = new Mat();
                dst = new Mat();
                src = abc;
                int[] arr = new int[4];
                arr = is_LED(src);
                //获取LED像素中心的位置
                int led_local_x = (arr[0] + arr[1]) / 2;
                int led_local_y = (arr[2] + arr[3]) / 2;
                //将原图中LED1部分的区域变黑
                double rowB = src.rows();//二值化图像的行数

                double colB = src.cols();//二值化图像的列数
                //求中心点位置
                width1 = colB / 2;
                height1 = rowB / 2;

                Mat matBinary1 = src.clone();//定义一幅图像来放去除LED1ROI之后的图

                for (int i = 0; i < rowB; i++) {
                    for (int j = 0; j < colB; j++) {
                        double r = Math.pow((i - led_local_y), 2) + Math.pow((j - led_local_x), 2) - Math.pow(((Math.abs(arr[1] - arr[0])) / 2 - 2), 2);//pow(x,y)计算x的y次方
                        if (r - 1000 > 0)//将r扩大，原来是360
                        {
                            //LED1圆外面像素重载为原图
                            matBinary1.put(i, j, src.get(i, j)[0]);
                        } else {
                            matBinary1.put(i, j, 0);//将第 i 行第 j 列像素值设置为255,二值化后为0和255
                        }
                    }
                }
                abc = matBinary1;
                Coordinate led = new Coordinate(arr[0], arr[1], led_local_x, arr[2], arr[3], led_local_y);
                LED.add(led);

            }


            LED_ROI1 = new LED_ROI(LED.get(0), 0,0, 0.0,  0);
            LED_ROI2 = new LED_ROI(LED.get(1), 0, 0, 0.0, 0);

            Rect rect1 = new Rect(LED.get(0).x_mMin, LED.get(0).y_mMin, LED.get(0).x_mMax - LED.get(0).x_mMin, LED.get(0).y_mMax - LED.get(0).y_mMin);
            Rect rect2 = new Rect(LED.get(1).x_mMin, LED.get(1).y_mMin, LED.get(1).x_mMax - LED.get(1).x_mMin, LED.get(1).y_mMax - LED.get(1).y_mMin);

            Mat dst1 = new Mat(bin_img, rect1);//二值化的图进行切割
            Mat dst2 = new Mat(bin_img, rect2);//二值化的图进行切割


            double r1, r2;
            r1 = banjing(dst1);//调用求取半长轴的函数，求取半长轴
            r2 = banjing(dst2);
            LED_ROI1.a = r1;
            LED_ROI2.a = r2;

            //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^计算条纹数目^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


            //计算条纹数
            List<MatOfPoint> contours1 = new ArrayList<>();
            Mat hierarchy1 = new Mat();
            Imgproc.findContours(dst1, contours1, hierarchy1, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);//找条纹数
            int LED_1 = contours1.size();
            LED_ROI1.LED_ID = LED_1;

            List<MatOfPoint> contours2 = new ArrayList<>();
            Mat hierarchy2 = new Mat();
            Imgproc.findContours(dst2, contours2, hierarchy2, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
            int LED_2 = contours2.size();
            LED_ROI2.LED_ID = LED_2;

            //定义ID
            //!!! （这里不用理会max与min）
            // !!!（只需要改变两支灯的坐标位置X,Y）
            position ID1 = new position(10, 0, 0, 0);//条纹数少的那个灯的位置信息
            position ID2 = new position(19, 11, 0, 30);//条纹数多的那个的位置信息

            ///ID识别
            //条纹少的所有的图像，位置信息传给LED_ONE,多的传给LED_TWO
            //LED_ROI LEDone,LEDtwo;分别传入条纹数目少的、多的、的ID信息
            LEDone = new LED_ROI(LED_ROI1.ROI, LED_ROI1.LED_ID, LED_ROI1.ID_X, LED_ROI1.ID_Y, LED_ROI1.a);
            LEDtwo = new LED_ROI(LED_ROI1.ROI, LED_ROI1.LED_ID, LED_ROI1.ID_X, LED_ROI1.ID_Y, LED_ROI1.a);

            //选择条纹数目少的作为LEDone
            if (LED_ROI1.LED_ID < LED_ROI2.LED_ID) {
                LED_ROI1.ID_X = ID1.X;
                LED_ROI1.ID_Y = ID1.Y;
                LEDone = LED_ROI1;
                LED_ROI2.ID_X = ID2.X;
                LED_ROI2.ID_Y = ID2.Y;
                LEDtwo = LED_ROI2;


            } else {
                LED_ROI1.ID_X = ID2.X;
                LED_ROI1.ID_Y = ID2.Y;
                LEDtwo = LED_ROI1;
                LED_ROI2.ID_X = ID1.X;
                LED_ROI2.ID_Y = ID1.Y;
                LEDone = LED_ROI2;
            }




            //  、、、、、、、、、、 下面输出一些检测数据的信息、、、、、、、、、、
            //、、、、、、、、、都是按照条纹数目少的、多的的顺序显示的、、、、、、
            System.out.println("图像中两个LED的中心值：*********************************");
            System.out.println("条纹数少的：*********************************");
            System.out.println(LEDone.ROI.x_mMiddle);//输出椭圆中心在图像上的位置信息
            System.out.println(LEDone.ROI.y_mMiddle);//输出椭圆中心在图像上的位置信息
            System.out.println("条纹数多的：*********************************");
            System.out.println(LEDtwo.ROI.x_mMiddle);//输出椭圆中心在图像上的位置信息
            System.out.println(LEDtwo.ROI.y_mMiddle);//输出椭圆中心在图像上的位置信息


            System.out.println("2个半长轴的大小：2222222222222222222222222222222222");
            System.out.println(LEDone.a);//半长轴输出测试
            System.out.println(LEDtwo.a);
//            tv1.setText(String.valueOf(LEDone.a));//之前测试半长轴来着
//            tv2.setText(String.valueOf(LEDtwo.a));
//            //  、、、、、、、、、、 下面输出一些检测数据的信息、、、、、、、、、、.

            //&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&求z坐标&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&7
            //焦距
            double f = 1.8;   //华为相机焦距，
            //f = f * 312.5;   //3.2e3的倒数,将f直接变过去，跟像素一个单位
            f=520;//这里原来是1.8*312.5=562.5，经过测试，可能520效果好一些，可能还需要根据相机进行更改
            //透镜焦点在image sensor上的位置
            double Center_X = width1;    ///////上面已经看过了，就是图像的中心(320,240)
            double Center_Y = height1;
            //显示一下图像中心的信息；


            //定义灯半径
            double r = 5.25;     //如果换灯具的话需要改，单位是cm
            double t11 = (double) Math.toDegrees(prefValues[1]);//   [1]为pitch，绕手机长轴
            double t22 = (double) Math.toDegrees(prefValues[2]);//   [1]为roll，绕手机短轴
            t11 = Math.abs(t11 / 180 * Math.PI);//先求出两个角的余弦值
            t22 = Math.abs(t22 / 180 * Math.PI);//先求出两个角的余弦值
            double t33 = Math.cos(t11) * Math.cos(t22);  //得到成像平面与水平面的夹角的余弦值
            double t = Math.acos(t33);//利用反三角函数，求成像平面与水平面的夹角，单位为弧度制（0-2pi）
            System.out.println("与竖直线夹角~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~··");
            System.out.println(t * 57.3);

            double Z = 0;
            double d1, d2;//（表示两个椭圆中心距离图像中心的距离）
            double h11, h12, h21, h22;//(计算的4个h的信息，两正两误)
            double z11, z12, z21, z22;//(计算的4个z的信息，两正两误)

            d1 = Math.sqrt(Math.pow((Center_X - LEDone.ROI.x_mMiddle), 2) + Math.pow((Center_Y - LEDone.ROI.y_mMiddle), 2));//求两个距离
            d2 = Math.sqrt(Math.pow((Center_X - LEDtwo.ROI.x_mMiddle), 2) + Math.pow((Center_Y - LEDtwo.ROI.y_mMiddle), 2));
            h11 = (f + d1 * Math.tan(t)) * Math.cos(t);//计算出4个h信息
            h12 = (f - d1 * Math.tan(t)) * Math.cos(t);
            h21 = (f + d2 * Math.tan(t)) * Math.cos(t);
            h22 = (f - d2 * Math.tan(t)) * Math.cos(t);

            z11 = h11 * r / LEDone.a;//进一步甄选出4个z信息
            z12 = h12 * r / LEDone.a;
            z21 = h21 * r / LEDtwo.a;
            z22 = h22 * r / LEDtwo.a;


            ///下面是计算出正确的Z值
            //在计算的4个结果中，寻找差值最小的两个Z值，求其平均值，作为最终的Z坐标的估计
            double[] c = new double[4];
            c[0] = Math.abs(z11 - z21);
            c[1] = Math.abs(z11 - z22);
            c[2] = Math.abs(z12 - z21);
            c[3] = Math.abs(z12 - z22);

            int d = 0;//看看是第几个数
            double xiao = c[0];
            for (int ii = 0; ii < 3; ii++) {
                if (xiao > c[ii + 1]) {
                    d = ii + 1;
                    xiao = c[ii + 1];
                }

            }

            //然后选出差值最小的两个Z求平均，作为最终Z坐标
            switch (d) {
                case 0:
                    Z = (z11 + z21) / 2;
                    break;
                case 1:
                    Z = (z11 + z22) / 2;
                    break;
                case 2:
                    Z = (z12 + z21) / 2;
                    break;
                case 3:
                    Z = (z12 + z22) / 2;
                    break;

            }
             //            double z1 = Z;
            int Z111 = (int) Z;//为了显示更加便利，特意转换为整形进行输出


            //&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&求z坐标&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&7

            //tv4.append("  z=");
            //tv4.append(String.valueOf(Z111));//(输出的手机距离天花板的高度，不是真正意义上的Z坐标)
            //tv4.setText(String.valueOf(Z111));





            //********************************求xy****************************************
            //最终位置初步
            double X1 = 0;
            double Y1 = 0;
            double X2 = 0;
            double Y2 = 0;

            double i1 = LEDone.ROI.x_mMiddle;//存放双灯的椭圆中心坐标
            double j1 = LEDone.ROI.y_mMiddle;
            double i2 = LEDtwo.ROI.x_mMiddle;
            double j2 = LEDtwo.ROI.y_mMiddle;

            double x1 = LEDone.ID_X;//存放双灯的真实位置信息
            double y1 = LEDone.ID_Y;
            double x2 = LEDtwo.ID_X;
            double y2 = LEDtwo.ID_Y;

            double a1 = LEDone.a;//双灯的半长轴
            double a2 = LEDtwo.a;

            //下面是求解连立方程

            //计算镜头中心到双灯的真实距离，并用徐海鑫的纠正方程加以纠正
            double L1=Math.sqrt(f * f + (i1 - Center_X) * (i1 - Center_X) + (j1 - Center_Y) * (j1 - Center_Y));
            double L2=Math.sqrt(f * f + (i2 - Center_X) * (i2 - Center_X) + (j2 - Center_Y) * (j2 - Center_Y));
            //进行纠正
            double s1=Math.sqrt((i1 - Center_X) * (i1 - Center_X) + (j1 - Center_Y) * (j1 - Center_Y))/312.5;
            L1=L1/(1-0.047*s1);
            double s2=Math.sqrt((i2 - Center_X) * (i2 - Center_X) + (j2 - Center_Y) * (j2 - Center_Y))/312.5;
            L2=L2/(1-0.047*s2);

            //m，n是为了方便解方程，中间计算过程的数据
            double m = (Math.pow((r / a1), 2) * L1*L1 - Z * Z);
            double n = (Math.pow((r / a2), 2) * L2*L2 - Z * Z);


            //由于灯具不同的坐标设置，有可能在求解方程组的时候，分母可能为0
            //(x1==x2的时候，方程按照下面这个判断的解法求解；
            if (x1 == x2) {
                Y1 = ((m - n) / (y2 - y1) + y1 + y2) / 2;
                Y2 = ((m - n) / (y2 - y1) + y1 + y2) / 2;
                X1 = x1 + Math.sqrt(m - (Y1 - y1) * (Y1 - y1));
                X2 = x1 - Math.sqrt(m - (Y1 - y1) * (Y1 - y1));

            }

            //(y1==y2的时候，方程按照下面这个判断的解法求解；
            else if (y1 == y2) {
                X1 = ((m - n) / (x2 - x1) + x1 + x2) / 2;
                X2 = ((m - n) / (x2 - x1) + x1 + x2) / 2;
                Y1 = y1 + Math.sqrt(m - (X1 - x1) * (X1 - x1));
                Y2 = y1 - Math.sqrt(m - (X1 - x1) * (X1 - x1));


            }

            //(x1!=x2,y1!=y2的时候，方程按照下面这个判断的解法求解；
            else {
                //y=kx+d10
                double k = (x2 - x1) / (y1 - y2);
                double d10 = (m - n + y2 * y2 - y1 * y1 + x2 * x2 - x1 * x1) / (2 * y2 - 2 * y1);

                double pp = 1 + k * k;
                double q = 2 * k * d10 - 2 * k * y1 - 2 * x1;
                double ss = x1 * x1 + (d10 - y1) * (d10 - y1) - m;

                X1 = (-q + Math.pow(q * q - 4 * pp * ss, 0.5)) / (2 * pp);
                X2 = (-q - Math.pow(q * q - 4 * pp * ss, 0.5)) / (2 * pp);
                Y1 = k * X1 + d10;
                Y2 = k * X2 + d10;
            }
            //至此求出两个方程的2个解

            System.out.println("最终1@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("两个解转换之前(联立求解的结果)");
            System.out.println(X1);
            System.out.println(Y1);
            System.out.println(X2);
            System.out.println(Y2);

            //下面是根据角度选择出正确解的过程

            double t1 = Math.atan2((y2 - y1), (x2 - x1));//用于坐标转换的角度(第一个考虑点)//双灯与X轴夹角，弧度制，范围-PI~PI，逆时针为正方向
            //注意，一定是2-1，反了的话就错了
            //把范围改为0~2PI（与X轴正方向夹角）    特别注意，计算正弦的时候，用弧度制（0~2pi）
            if (t1 < 0) {
                t1 = t1 + 2 * Math.PI;
            }

//            System.out.print("双灯与x轴正方向夹角********(0-360)");
//            System.out.print(t1 * 57.3);//弧度制与角度制之间的转换

            t1 = -t1;//对解出的结果进行坐标转换
            double X11, Y11, X22, Y22;
            X11 = X1 * Math.cos(t1) - Y1 * Math.sin(t1);
            Y11 = X1 * Math.sin(t1) + Y1 * Math.cos(t1);
            X22 = X2 * Math.cos(t1) - Y2 * Math.sin(t1);
            Y22 = X2 * Math.sin(t1) + Y2 * Math.cos(t1);

            //再把t1变回来
            t1 = -t1;
            System.out.println("两个解转换之后（此时两解是关于x轴对称的）^^^^^^^^^^^^^^^^^^^^^^^^^^");
            System.out.println(X11);
            System.out.println(Y11);
            System.out.println(X22);
            System.out.println(Y22);


            //存放左右侧选择的坐标
            double X3 = 0;    //存放右侧，下侧（以X轴正方向为基准）
            double Y3 = 0;
            double X4 = 0;    //存放左侧、上侧
            double Y4 = 0;

            if (Y11 > Y22) {
                X4 = X1;
                Y4 = Y1;
                X3 = X2;
                Y3 = Y2;
            } else {
                X3 = X1;
                Y3 = Y1;
                X4 = X2;
                Y4 = Y2;
            }
            //已经选出了左、右侧

            double X5 = 0, Y5 = 0; //存放最终确定的XY坐标

            ///再决定是否再次翻转
            t11 = (double) Math.toDegrees(prefValues[1]);//   [1]为pitch，绕长轴
            t22 = (double) Math.toDegrees(prefValues[2]);//   [1]为roll，绕短轴
            double t2 = prefValues[0];// Z轴旋转角()
            //因为调用的是prefValues[0]  ，所以t2范围是-180~180
            //由于不知道调用的那个传感器。为了防止出错，所以进行一个判断，使得读入的地磁偏角一直是正确的（与正北0~360）
//            System.out.print("t2输出测试****************");
//            System.out.print(t2);
            if (t2 < 0) {
                t2 = t2 + 360;//假设z的旋转角和世界坐标系的X轴一致
            }


            t1 = t1 * 57.29578;  //转为0-360之间的数据
            //再把t1转换成与Y轴正方向的夹角，此时顺时针为正方向
            t1 = 450 - t1;
            if (t1 >= 360) {
                t1 = t1 - 360;
            }

            //
            ///////////////////////((((((((((((((((((((((((((((((((((((((((((((((((((((((((((
            //在这里考虑，Y轴与正北方向的偏差,需要手动改变
            //输入一个0-360之间的度数 t4，规定其方向与传感器方向一致，
            /////////////))))))))))))))))))))))))))))))))))
            double t4 = 90;    //用手机的指南针测量这个数据,Y轴正方向与正北夹角，0~360；


//            System.out.print("t2****************************");
//            System.out.print(t2);
            //t2转换成手机与Y轴的夹角，t1仍然是与Y轴的夹角
            t2 = t2 - t4;

            if (t2 < 0) {
                t2 = t2 + 360;//理论上，t2变成了手机与Y轴的夹角（0~360）
            }
            System.out.print("t2  地磁偏角****************************");
            System.out.print(t2);


            double t3 = Math.abs(t1 - t2);//计算双灯向量和相机的夹角
            int t333 = (int) t3;//转换成int类型，方便输出
//            tv0.append(" 角度=");
//            tv0.append(String.valueOf(t333));

            int t222 = (int) t22;
//            tv00.append(" 左右=");
//            tv00.append(String.valueOf(t222));1
            if (t22 >= 0) {          //手机右侧倾斜
                if (t3 > 90 && t3 < 270)      //反向，选左侧的
                {
                    X5 = X3;
                    Y5 = Y3;
                } else                               //选右侧的
                {
                    X5 = X4;
                    Y5 = Y4;
                }
            } else {
                if (t3 > 90 && t3 < 270)      //选右侧的
                {
                    X5 = X4;
                    Y5 = Y4;
                } else                               //选左侧的
                {
                    X5 = X3;
                    Y5 = Y3;
                }

            }


            System.out.println("根据角度选择出的结果&&&&&&&&&&&&&&&&&&&&&&&&&&");
            System.out.println(X5);
            System.out.println(Y5);

            //数据类型转换
            int X555 = (int) X5;
            int Y555 = (int) Y5;


            //显示最后的选择的结果
            //这是原来的输出方法，先保存着
            //tv5.append("  x1=");
            //tv5.append(String.valueOf(X555));
            //tv6.append("  y1=");
            //tv6.append(String.valueOf(Y555));

            //tv5.setText("123");
//            tv5.setText(String.valueOf(X555));
//            tv6.setText(String.valueOf(Y555));


            //********************************求xy****************************************



            nx = X5;
//            nx=0;
            ny = Y5;
            nz = Z111;





        } catch (Exception e) {
            e.printStackTrace();
            nx = 1;
            ny = 1;
            nz = 1;

        }
        //

        matToBitmap(src, selectbp);


    }















    /////////////////////自己加的子函数调用



    //求半长轴的函数<banjing()>
    public static double banjing(Mat mat){
        int row=mat.rows()-1;//为了保障计算，所以减了一个1；
        int col=mat.cols()-1;
        double xlocal=col/2;
        double ylocal=row/2;
        double r1=1.1;
        double r2=1.2;
        for(int i=0;i<row;i++){
            for(int j=0;j<col;j++) {
                int a=(int)mat.get(i,j)[0];
                if(a>10)  //二值化图形只有0和255，任意定了一个值。此时选出最远的距离
                {
                    r1 = Math.sqrt(Math.pow((i - ylocal), 2) + Math.pow((j - xlocal), 2));
                    if (r2 < r1)
                        r2 = r1;   //挑选出最大值放给r2
                }
            }
        }
        return r2;//返回半长轴
    }











    ///分割LED
    public static int [] is_LED(Mat mat){
        int X_min, X_max, Y_min , Y_max;
//        Mat LED_ROI=new Mat();

        Mat temp1= mat.clone();
        //求输入Mat的行数列数
        int row1=temp1.rows();
        int col1=temp1.cols();

        int j=0;//注意从0开始
        while (j < col1){//j的初值为1
            double sum1=0.0;
            for(int i=0;i<row1;i++)//注意没有等号
            {
                double data = (int) temp1.get(i, j)[0];
                sum1=sum1+data;
            }//将第j列的每一行加完
            if (sum1>-0.000001 && sum1< 0.000001)//double类型，不能写==0
            {
                j++;
            }
            else
            {
                break;//跳出这个while循环，第一次检测到白条纹了
            }
        }
        X_min=j;

        while (j < col1)//j的初值为X_min
        {
            double sum1 = 0.0;
            for (int i = 0;i < row1;i++)
            {
                double data = (int) temp1.get(i, j)[0];
                sum1=sum1+data;
            }//将第j列的每一行XXXXXX加完
            if (sum1 != 0)
            {
                j++;
            }
            else
            {
                break;//再次检测到黑条纹时，跳出这个while循环
            }
        }
        X_max = j;
//        X.mMiddle=(X.mMin+X.mMax)/2;

        //进行ROI切割
//        Mat image_cut = mat(Rect(X.mMin, 0, X.mMax - X.mMin, row));
        Rect rect=new Rect(X_min,0,X_max - X_min,row1);
        Mat image_cut=new Mat(mat,rect);
        Mat temp = image_cut.clone();

        //求ymin与ymax
        int row = temp.rows();//行数
        int col = temp.cols();//列
        int i = 0;
        while (i < row)//i的初值为1
        {
            double sum = 0.0;
            for (j = 0;j < col;j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
            {
                double data = (int) temp.get(i, j)[0];
                sum = data + sum;
            }//最终获得第i行的列和
            if (sum>-0.000001 && sum < 0.000001)
            {
                i++;
            }
            else
            {
//                Y_min = i;
                break;//跳出这个while循环
            }
        }
        Y_min = i;

        while (i <= row-16)//i的初值为Y_min
        {
            double sum = 0.0;
//            uchar* data = temp.ptr<uchar>(i);
            for (j = 0;j < col;j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
            {
                double data = (int) temp.get(i, j)[0];
                sum = data + sum;
            }//最终获得第i行的列和
            if (sum != 0)
            {
                i++;
            }
            else
            {
                double sum6 = 0.0;
                int iiii = i + 16;
//                uchar* data = temp.ptr<uchar>(iiii);
                for (j = 0;j < col;j++)//对每一行中的每一列像素进行相加，ptr<uchar>(i)[j]访问第i行第j列的像素
                {
                    double data = (int) temp.get(iiii, j)[0];
                    sum6 = data + sum6;
                }//最终获得第i行之后20行，即iiii的列和
                if (sum6 > -0.000001 && sum6 < 0.000001)//如果仍然为0，才跳出
                {
//                    Y_max = i;
                    break;//跳出这个while循环
                }
                else//否则继续执行
                {
                    i++;
                }
            }
        }
        Y_max = i;

        //进行切割
//        Rect rect1= new Rect(0, Y_min, col, Y_max - Y_min);
//        Mat image_cut1=new Mat(temp,rect1);
//        Mat image_cut1 = new Mat (temp,new Rect(0, Y_min, col, Y_max - Y_min));
//        Mat img_next = image_cut1.clone();   //clone函数创建新的图片
        int[] array={X_min, X_max, Y_min , Y_max};
        return array;

//        LED_ROI=img_next;
    }


















    /////定义LED-ID的结构体
    public class position{// LED的位置，对应不同位置的灯具
        int max;	// ID_max,最大条纹数目
        int min;	// ID_min，最小条纹数目
        double X;	// LED灯具的真实位置,x坐标
        double Y;	// LED灯具的真实位置,y坐标

        public position(int _max, int _min,double _X, double _Y ) {
            max=_max;
            min=_min;
            X=_X;
            Y=_Y;
        }
    };











    ///定义存放ROI的数组（最大、最小、以及中间）
    public class Coordinate {
        private int x_mMin;
        private int x_mMax;
        private int x_mMiddle;
        private int y_mMin;
        private int y_mMax;
        private int y_mMiddle;

        public Coordinate(int x_min, int x_max, int x_middle,int y_min, int y_max, int y_middle) {
            x_mMin = x_min;
            x_mMax = x_max;
            x_mMiddle = x_middle;
            y_mMin = y_min;
            y_mMax = y_max;
            y_mMiddle = y_middle;
        }

        public int getMinx_() {
            return x_mMin;
        }

        public void setMinx_(int x_min) {
            x_mMin = x_min;
        }

        public int getMaxx_() {
            return x_mMax;
        }

        public void setMaxx_(int x_max) {
            x_mMax = x_max;
        }

        public int getMiddlex_() {
            return x_mMiddle;
        }

        public void setMiddlex_(int x_middle) {
            x_mMiddle = x_middle;
        }

        public int getMiny_() {
            return y_mMin;
        }

        public void setMiny_(int y_min) {
            y_mMin = y_min;
        }

        public int getMaxy_() {
            return y_mMax;
        }

        public void setMaxy_(int y_max) {
            y_mMax = y_max;
        }

        public int getMiddley_() {
            return y_mMiddle;
        }

        public void setMiddley_(int y_middle) {
            y_mMiddle = y_middle;
        }

    }







    //定义LED-ROI结构体
    public class LED_ROI{
        public Coordinate ROI;
        public int LED_ID;
        public double ID_X;
        public double ID_Y;
        public double a;

        public LED_ROI(Coordinate _ROI, int _LED_ID, double _ID_X, double _ID_Y,double a1){
            ROI=_ROI;
            LED_ID=_LED_ID;
            ID_X=_ID_X;
            ID_Y=_ID_Y;
            a=a1;
        }

        public Coordinate getroi_() {
            return ROI;
        }

        public void setroi_(Coordinate _ROI) {
            ROI = _ROI;
        }
        public int getid_() {
            return LED_ID;
        }

        public void setid_(int _LED_ID) {
            LED_ID = _LED_ID;
        }

    }







}
