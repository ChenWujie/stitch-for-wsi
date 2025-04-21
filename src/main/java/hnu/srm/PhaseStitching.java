package hnu.srm;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class PhaseStitching {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        // 加载两幅图像
        Mat image1 = Imgcodecs.imread("src/photo/0_00000.tif");
        Mat image2 = Imgcodecs.imread("src/photo/0_00001.tif");

        if (image1.empty() || image2.empty()) {
            System.out.println("无法加载图像！");
            return;
        }

        Mat imgACrop, imgBCrop;
        // 计算重叠部分的宽度（假设重叠部分为10%）
        int overlap;

        // 裁剪出图像的重叠部分:左右
        overlap = (int) (image1.cols() * 0.3);
        imgACrop = new Mat(image1, new Rect(image1.cols() - overlap, 0, overlap, image1.rows()));
        imgBCrop = new Mat(image2, new Rect(0, 0, overlap, image2.rows()));

        // 将图像转换为灰度图
        Mat gray1 = new Mat();
        Mat gray2 = new Mat();
        Imgproc.cvtColor(imgACrop, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(imgBCrop, gray2, Imgproc.COLOR_BGR2GRAY);

        // 将图像转换为浮点类型
        Mat gray1Float = new Mat();
        Mat gray2Float = new Mat();
        gray1.convertTo(gray1Float, CvType.CV_32F);
        gray2.convertTo(gray2Float, CvType.CV_32F);

        // 将图像转换为频域
        Mat dft1 = new Mat();
        Mat dft2 = new Mat();
        Core.dft(gray1Float, dft1, Core.DFT_COMPLEX_OUTPUT, 0);
        Core.dft(gray2Float, dft2, Core.DFT_COMPLEX_OUTPUT, 0);

        // 计算交叉功率谱
        Mat crossPowerSpectrum = new Mat();
        Core.mulSpectrums(dft1, dft2, crossPowerSpectrum, 0, true);
        Core.normalize(crossPowerSpectrum, crossPowerSpectrum);

        // 计算逆傅里叶变换得到相位相关结果
        Mat inverseDFT = new Mat();
        Core.idft(crossPowerSpectrum, inverseDFT, Core.DFT_SCALE | Core.DFT_REAL_OUTPUT, 0);

        // 找到峰值位置
        Core.MinMaxLocResult mmr = Core.minMaxLoc(inverseDFT);
        Point shift = mmr.maxLoc;

        System.out.println("检测到的位移: x = " + shift.x + ", y = " + shift.y);

        // 创建一个新的图像，用于存放拼接后的结果
        int width = image1.cols() + image2.cols();
        int height = 2 * image1.rows();

        Mat stitched = new Mat(new Size(width, height), image1.type());

        // 将第一幅图像复制到拼接图像中
        image1.copyTo(stitched.rowRange(0, image1.rows()).colRange(0, image1.cols()));

        // 根据计算的位移将第二幅图像放入拼接图像中
        int xOffset = image1.cols() - overlap + (int)Math.max(0, shift.x);
        int yOffset = (int)Math.max(0, shift.y);

        image2.copyTo(stitched.rowRange(yOffset, yOffset + image2.rows()).colRange(xOffset, xOffset + image2.cols()));

        // 保存拼接结果
        Imgcodecs.imwrite("stitched_result.jpg", stitched);

        System.out.println("拼接完成，结果已保存为 stitched_result.jpg");
    }
}
