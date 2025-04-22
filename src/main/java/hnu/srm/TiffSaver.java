package hnu.srm;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.io.FileSaver;
import ij.ImageStack;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.Size;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class TiffSaver {

    // 分块保存 TIFF 图像
    public static void saveTiffInChunks(Mat largeMat, String outputPath, int blockSize) {
        int rows = largeMat.rows();
        int cols = largeMat.cols();

        try {
            // 创建 ImageStack 来存放所有分块
            ImageStack stack = new ImageStack(cols, blockSize);

            // 遍历每个图像块
            for (int rowStart = 0; rowStart < rows; rowStart += blockSize) {
                int rowEnd = Math.min(rowStart + blockSize, rows);

                // 截取每个分块
                Mat block = largeMat.submat(rowStart, rowEnd, 0, cols);
                BufferedImage bufferedImage = matToBufferedImage(block);

                // 将 BufferedImage 转换为 ImageProcessor
                ImageProcessor ip = new ij.process.ColorProcessor(bufferedImage);

                // 将 ImageProcessor 添加到 ImageStack 中
                stack.addSlice("Slice " + rowStart, ip);
            }

            // 最后将整个 ImageStack 保存为一个单独的 TIFF 文件
            ImagePlus imp = new ImagePlus("", stack);
            FileSaver saver = new FileSaver(imp);
            saver.saveAsTiff(outputPath);  // 保存为单个 TIFF 文件

            System.out.println("TIFF saved successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // 将 Mat 转换为 BufferedImage
    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        byte[] data = new byte[width * height * mat.channels()];
        mat.get(0, 0, data);
        bufferedImage.getRaster().setDataElements(0, 0, width, height, data);

        return bufferedImage;
    }

    public static void main(String[] args) {
        // 假设 largeMat 是你的超大 Mat 图像
        Mat largeMat = new Mat(70000, 50000, CvType.CV_8UC3); // 示例，需根据你的数据来设置
        saveTiffInChunks(largeMat, "output.tif", 1000);  // 每块高度为 1000
    }
}
