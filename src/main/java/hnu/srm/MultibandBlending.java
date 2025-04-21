package hnu.srm;

import org.opencv.core.*;
//import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MultibandBlending {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // 读取图像
        Mat image1 = Imgcodecs.imread("src/main/java/photo/20230330_1/0_00063.tif");
        Mat image2 = Imgcodecs.imread("src/main/java/photo/20230330_1/0_00064.tif");

        if (image1.empty() || image2.empty()) {
            System.out.println("无法读取图像");
            return;
        }

        if (image1.empty() || image2.empty()) {
            System.out.println("无法读取图像");
            return;
        }

        // 确保图像大小一致
        if (!image1.size().equals(image2.size())) {
            System.out.println("图像大小不一致，调整图像大小");
            Imgproc.resize(image2, image2, image1.size());
        }

        // 创建掩膜
        Mat mask = createMask(image1.size());

        // 确保掩膜大小一致
        if (!mask.size().equals(image1.size())) {
            Imgproc.resize(mask, mask, image1.size());
        }

        // 多频段融合
        Mat blendedImage = multibandBlend(image1, image2, mask, 5);

        // 保存结果
//        Imgcodecs.imwrite("path/to/blended_image.jpg", blendedImage);
        System.out.println("图像融合成功并保存到：path/to/blended_image.jpg");
    }

    private static Mat createMask(Size size) {
        Mat mask = new Mat(size, CvType.CV_8UC1, new Scalar(0));
        int cols = (int) size.width;
        mask.colRange(cols / 4, cols * 3 / 4).setTo(new Scalar(255));
        return mask;
    }

    private static Mat multibandBlend(Mat image1, Mat image2, Mat mask, int levels) {
        List<Mat> pyrImage1 = buildLaplacianPyramid(image1, levels);
        List<Mat> pyrImage2 = buildLaplacianPyramid(image2, levels);
        List<Mat> pyrMask = buildGaussianPyramid(mask, levels);

        List<Mat> pyrBlended = new ArrayList<>();
        for (int i = 0; i < levels; i++) {
            Mat blended = new Mat();

            // 确保所有输入矩阵的大小和通道数一致
            Size size = pyrImage1.get(i).size();
            Imgproc.resize(pyrImage2.get(i), pyrImage2.get(i), size);
            Imgproc.resize(pyrMask.get(i), pyrMask.get(i), size);

            Core.addWeighted(pyrImage1.get(i), 1.0, pyrImage2.get(i), 1.0, 0.0, blended);

            // 将掩膜转换为三通道
            Mat mask3Channel = new Mat();
            Imgproc.cvtColor(pyrMask.get(i), mask3Channel, Imgproc.COLOR_GRAY2BGR);

            // 将掩膜转换为浮点类型并归一化
            Mat maskFloat = new Mat();
            mask3Channel.convertTo(maskFloat, CvType.CV_32FC3, 1.0 / 255.0);

            // 使用三通道掩膜进行乘法操作
            Mat blendedMasked = new Mat();
            Core.multiply(blended, maskFloat, blendedMasked);

            pyrBlended.add(blendedMasked);
        }

        Mat blendedImage = pyrBlended.get(levels - 1);
        for (int i = levels - 2; i >= 0; i--) {
            Size size = pyrBlended.get(i).size();
            Imgproc.pyrUp(blendedImage, blendedImage, size);
            Core.add(blendedImage, pyrBlended.get(i), blendedImage);
        }

        return blendedImage;
    }

    private static List<Mat> buildGaussianPyramid(Mat image, int levels) {
        List<Mat> pyramid = new ArrayList<>();
        pyramid.add(image);
        Mat current = image;
        for (int i = 1; i < levels; i++) {
            Mat down = new Mat();
            Imgproc.pyrDown(current, down);
            pyramid.add(down);
            current = down;
        }
        return pyramid;
    }

    private static List<Mat> buildLaplacianPyramid(Mat image, int levels) {
        List<Mat> gaussianPyramid = buildGaussianPyramid(image, levels);
        List<Mat> laplacianPyramid = new ArrayList<>();
        for (int i = 0; i < levels - 1; i++) {
            Mat up = new Mat();
            Imgproc.pyrUp(gaussianPyramid.get(i + 1), up, gaussianPyramid.get(i).size());
            Mat laplacian = new Mat();
            Core.subtract(gaussianPyramid.get(i), up, laplacian);
            laplacianPyramid.add(laplacian);
        }
        laplacianPyramid.add(gaussianPyramid.get(levels - 1));
        return laplacianPyramid;
    }
}