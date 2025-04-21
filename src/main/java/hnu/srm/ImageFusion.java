package hnu.srm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageFusion {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        String[] imagePaths = {"src/main/java/photo/up.jpg", "src/main/java/photo/down.jpg", "src/main/java/photo/left.jpg", "src/main/java/photo/right.jpg"};
        int numImages = imagePaths.length;
        Mat[] images = new Mat[numImages];
        int[][] positions = new int[numImages][2];  // 假设图像都是2D的
        float[] weights = new float[numImages];
        int[] minDistance = new int[numImages];
        double alpha = 1.0;  // 调整alpha值

        for (int i = 0; i < numImages; i++) {
            images[i] = Imgcodecs.imread(imagePaths[i], Imgcodecs.IMREAD_GRAYSCALE);
            positions[i][0] = 0;  // 假设图像的位置
            positions[i][1] = 0;
        }

        // 假设要计算位置(50, 50)处的权重
        int[] pos = {1000, 1210};

        computeLinearWeights(images, positions, numImages, pos, weights, minDistance, alpha);

        for (int i = 0; i < numImages; i++) {
            System.out.println("Weight for image " + i + ": " + weights[i]);
        }
    }

    private static void computeLinearWeights(Mat[] images, int[][] positions, int num, int[] pos, float[] weights, int[] minDistance, double alpha) {
        if (num == 1) {
            weights[0] = 1;
            return;
        }

        float sumInverseWeights = 0;
        for (int i = 0; i < num; i++) {
            minDistance[i] = 1;
            for (int dim = 0; dim < pos.length; dim++) {
                int localImgPos = pos[dim] - positions[i][dim];
                int value = Math.min(localImgPos, (int)images[i].size().width - localImgPos - 1) + 1;
                minDistance[i] *= value;
            }

            minDistance[i]++;
            weights[i] = (float) Math.pow(minDistance[i], alpha);
            sumInverseWeights += weights[i];
        }

        for (int i = 0; i < num; i++) {
            weights[i] /= sumInverseWeights;
        }
    }
}
