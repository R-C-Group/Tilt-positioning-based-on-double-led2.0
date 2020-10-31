package com.example.xianchengqiege;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.lang.StrictMath.pow;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.contourArea;


public class Photo_processing {
    public double nx = 1;
    public double ny = 1;
    private double PixelSize = 0.0032;
    private double focalLength = 1.03319f;
    private double r = 5;
    public Bitmap selectbp;
    public double d1=0,d2=0,d3=0;





    public Photo_processing(Bitmap bitmap) {
        selectbp = bitmap;
        FeaturePoint(selectbp);
    }


    private void FeaturePoint(Bitmap bmp) {
        Point center;
        Mat gray_img = new Mat(), bin_img = new Mat();
        Mat src = new Mat();
        Mat closed = new Mat();
        Mat element;
        Mat blurred = new Mat();
        Mat edges = new Mat();


        bitmapToMat(bmp,src);


        double ie[] = {0, 0, 0};
        double je[] = {0, 0, 0};//LED在图像中中心的位置
        double l[] = {0, 0, 0};
        double s[] = {0, 0, 0};//LED椭圆长短轴
        double ww[] = {0, 0, 0};//椭圆倾角
        double j0 = src.rows() / 2;
        double i0 = src.cols() / 2;//图像中点坐标

        try {
        //图像处理
        Imgproc.cvtColor(src, gray_img, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray_img, bin_img, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgproc.GaussianBlur(bin_img, blurred, new Size(9, 9), 0);
        element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(20, 20));
        Imgproc.morphologyEx(blurred, closed, Imgproc.MORPH_CLOSE, element);
        Scalar p[] = {new Scalar(255, 0, 0), new Scalar(0, 255, 0), new Scalar(0, 0, 255)};
        Imgproc.Canny(closed, edges, 40, 120, 3);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();// 创建容器，存储轮廓
        Mat hierarchy = new Mat();// 寻找轮廓所需参数
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        int j = 0;

        for (int i = 0; i < min(contours.size(),3); i++) {
            RotatedRect m_ellipsetemp;  // fitEllipse返回值的数据类型
            if (contourArea(contours.get(i)) < 15 && contourArea(contours.get(i)) > 100000) {
                continue;
            }
            MatOfPoint2f point2f = new MatOfPoint2f(contours.get(i).toArray());
            m_ellipsetemp = Imgproc.fitEllipse(point2f);  //找到的第一个轮廓，放置到m_ellipsetemp
            Imgproc.ellipse(src, m_ellipsetemp, p[j],2,2);   //在图像中绘制椭圆
            center = m_ellipsetemp.center;//读取椭圆中心
//            Imgproc.drawContours(src, contours, i, new Scalar(255, 0, 0), 60, 1);//绘制椭圆中心
            ie[j] = center.x - i0;
            je[j] = j0 - center.y;
            l[j] = m_ellipsetemp.size.height / 2;
            s[j] = m_ellipsetemp.size.width / 2;
            ww[j] = m_ellipsetemp.angle / 180;
            j = j + 1;
        }


        //LED1
        double p1 = sqrt(ie[0] * ie[0] + je[0] * je[0]) * PixelSize;
        double H1 = focalLength * r / (l[0] * PixelSize);
        double o1 = H1 / focalLength * p1;
        d1 = sqrt(o1 * o1 + H1 * H1);


        //LED2
        double p2 = sqrt(ie[1] * ie[1] + je[1] * je[1]) * PixelSize;
        double H2 = focalLength * r / (l[1] * PixelSize);
        double o2 = H2 / focalLength * p2;
        d2 = sqrt(o2 * o2 + H2 * H2);



        //LED3
        double p3 = sqrt(ie[2] * ie[2] + je[2] * je[2]) * PixelSize;
        double H3 = focalLength * r / (l[2] * PixelSize);
        double o3 = H2 / focalLength * p3;
        d3 = sqrt(o3 * o3 + H3 * H3);


        double x1 = 60, x2 = 0, x3 = 0;
        double y1 = 90, y2 = 90, y3 = 0;


            double r1 = pow(d1, 2);
            double r2 = pow(d2, 2);
            double r3 = pow(d3, 2);

            double a1 = 2 * (x1 - x3);
            double b1 = 2 * (y1 - y3);
            double c1 = pow(x3, 2) - pow(x1, 2) + pow(y3, 2) - pow(y1, 2) - r3 + r1;
            double a2 = 2 * (x2 - x3);
            double b2 = 2 * (y2 - y3);
            double c2 = pow(x3, 2) - pow(x2, 2) + pow(y3, 2) - pow(y2, 2) - r3 + r2;

//            double XX = (c2 * b1 - c1 * b2) / (a1 * b2 - a2 * b1);
//            double YY = (c2 * a1 - c1 * a2) / (a2 * b1 - a1 * b2);


            double XX=((y1 - y2)*(d1*d1 - d3*d3 - x1*x1 + x3*x3 - y1*y1 + y3*y3))/(2*(x1*y2 - x2*y1 - x1*y3 + x3*y1 + x2*y3 - x3*y2)) - ((y1 - y3)*(d1*d1 - d2*d2 - x1*x1 + x2*x2 - y1*y1 + y2*y2))/(2*(x1*y2 - x2*y1 - x1*y3 + x3*y1 + x2*y3 - x3*y2));


            double YY= ((x1 - x3)*(d1*d1 - d2*d2 - x1*x1 + x2*x2 - y1*y1 + y2*y2))/(2*(x1*y2 - x2*y1 - x1*y3 + x3*y1 + x2*y3 - x3*y2)) - ((x1 - x2)*(d1*d1 - d3*d3 - x1*x1 + x3*x3 - y1*y1 + y3*y3))/(2*(x1*y2 - x2*y1 - x1*y3 + x3*y1 + x2*y3 - x3*y2));



            nx = XX;
            ny = YY;
//            d1=l[0]*PixelSize;




        } catch (Exception e) {
            e.printStackTrace();
            nx=1;
            ny=1;
            d1=0;
            d2=0;
            d3=0;
        }
        //

        matToBitmap(src,selectbp);

    }











}
