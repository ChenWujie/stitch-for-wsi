package hnu.srm;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class NccBasedStitch {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // 加载需要拼接的图像
        ArrayList<Mat> images = new ArrayList<>();
        images.add(Imgcodecs.imread("src/photo/left.jpg"));
        images.add(Imgcodecs.imread("src/photo/right.jpg"));
        // 添加更多图像...

        // 计算NCC并找到最佳拼接位置
        int[][] overlapRegion = findOverlapRegion(images.get(0), images.get(1));
        int dx = overlapRegion[0][1] - overlapRegion[0][0];
        int dy = overlapRegion[1][1] - overlapRegion[1][0];

        // 拼接图像
        Mat result = stitchImages(images, dx, dy);

        // 保存拼接后的图像
        HighGui.imshow("re", result);
        HighGui.waitKey();
    }

    private static int[][] findOverlapRegion(Mat img1, Mat img2) {
        // 使用归一化互相关(NCC)计算最佳重叠区域
        int width1 = img1.cols();
        int height1 = img1.rows();
        int width2 = img2.cols();
        int height2 = img2.rows();

        Mat corr = new Mat(height1 - height2 + 1, width1 - width2 + 1, CvType.CV_32FC1);
        Imgproc.matchTemplate(img1, img2, corr, Imgproc.TM_CCOEFF_NORMED);

        // 找到最大相关值对应的坐标
        Core.MinMaxLocResult mmr = Core.minMaxLoc(corr);
        Point maxLoc = mmr.maxLoc;

        // 返回重叠区域的坐标范围
        return new int[][] {{(int) maxLoc.x, (int) maxLoc.x + width2},
                {(int) maxLoc.y, (int) maxLoc.y + height2}};
    }

    private static Mat stitchImages(ArrayList<Mat> images, int dx, int dy) {
        // 根据计算的重叠区域大小,创建拼接后的图像
        int totalWidth = 0;
        int maxHeight = 0;
        for (Mat img : images) {
            totalWidth += img.cols() - dx;
            maxHeight = Math.max(maxHeight, img.rows());
        }
        Mat result = new Mat(maxHeight, totalWidth, CvType.CV_8UC3);

        // 将图像逐个拼接到结果图像上
        int x = 0;
        for (Mat img : images) {
            Rect roi = new Rect(x, 0, img.cols() - dx, img.rows());
            img.copyTo(result.submat(roi));
            x += img.cols() - dx;
        }

        return result;
    }
}