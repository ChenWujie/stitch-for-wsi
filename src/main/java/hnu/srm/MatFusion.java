package hnu.srm;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Core;

import java.util.ArrayList;
import java.util.List;

public class MatFusion {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // 读取图像
        Mat matA = Imgcodecs.imread("src/main/java/photo/20230330_1/0_00063.tif");
        Mat matB = Imgcodecs.imread("src/main/java/photo/20230330_1/0_00063.tif");

        if (matA.empty() || matB.empty()) {
            System.out.println("无法读取图像");
            return;
        }

        // 确保两个矩阵的大小和类型一致
        if (!matA.size().equals(matB.size()) || matA.type() != matB.type()) {
            System.out.println("矩阵大小或类型不一致");
            return;
        }
        Mat temp = new Mat(matA.height()/2, matA.width()/2, matA.type(), Scalar.all(0));
        Mat roi = matA.submat(new Rect(0, 0, matA.width()/2, matA.rows()/2));
        temp.copyTo(roi);
//        HighGui.imshow("a", matA);
//        HighGui.waitKey();

        // 分离RGB通道
        List<Mat> channelsA = new ArrayList<>();
        List<Mat> channelsB = new ArrayList<>();
        Core.split(matA, channelsA);
        Core.split(matB, channelsB);

        List<Mat> resultChannels = new ArrayList<>();

        // 对每个通道进行处理
        for (int i = 0; i < 3; i++) {
            Mat channelA = channelsA.get(i);
            Mat channelB = channelsB.get(i);

            // 创建掩膜：matA为0的位置为True，其余为False
            Mat mask = new Mat();
            Core.compare(channelA, new Scalar(0), mask, Core.CMP_EQ);

            // 将掩膜转换为浮点型
            Mat maskFloat = new Mat();
            mask.convertTo(maskFloat, CvType.CV_32F, 1.0 / 255.0);

            // 转换到浮点型
            Mat channelAFloat = new Mat();
            Mat channelBFloat = new Mat();
            channelA.convertTo(channelAFloat, CvType.CV_32F);
            channelB.convertTo(channelBFloat, CvType.CV_32F);

            // 计算均值
            Mat sum = new Mat();
            Core.add(channelAFloat, channelBFloat, sum);
            Mat mean = new Mat();
            Core.multiply(sum, new Scalar(0.5), mean);

            // 计算融合结果
            Mat resultChannel = new Mat();
            Mat blended = new Mat();
            Core.multiply(channelBFloat, maskFloat, blended);
            Core.add(blended, mean, resultChannel);

            resultChannels.add(resultChannel);
        }

        // 合并通道
        Mat result = new Mat();
        Core.merge(resultChannels, result);

        // 将结果转换回8位图像
        Mat finalResult = new Mat();
        result.convertTo(finalResult, CvType.CV_8UC3);

        // 保存结果
        HighGui.imshow("r", finalResult);
        HighGui.waitKey();
        System.out.println("结果图像保存到：path/to/result.jpg");
    }
}
