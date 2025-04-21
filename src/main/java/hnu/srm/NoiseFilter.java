package hnu.srm;

import java.util.ArrayList;
import java.util.List;

public class NoiseFilter {
    public static double[] filterNoiseAndCalculateMean(double[][] data) {
        double[] mean = calculateMean(data);
        double[] stdDev = calculateStandardDeviation(data, mean);

        // 设定阈值，这里使用2倍标准差作为剔除噪声点的标准
        double thresholdDx = 2 * stdDev[0];
        double thresholdDy = 2 * stdDev[1];

        List<double[]> filteredData = new ArrayList<>();

        for (double[] point : data) {
            if (Math.abs(point[0] - mean[0]) <= thresholdDx && Math.abs(point[1] - mean[1]) <= thresholdDy) {
                filteredData.add(point);
            }
        }

        double[][] filteredDataArray = filteredData.toArray(new double[0][]);
        return calculateMean(filteredDataArray);
    }

    private static double[] calculateMean(double[][] data) {
        double sumDx = 0;
        double sumDy = 0;
        for (double[] point : data) {
            sumDx += point[0];
            sumDy += point[1];
        }
        return new double[]{ sumDx / data.length, sumDy / data.length };
    }

    private static double[] calculateStandardDeviation(double[][] data, double[] mean) {
        double sumSquaredDx = 0;
        double sumSquaredDy = 0;
        for (double[] point : data) {
            sumSquaredDx += Math.pow(point[0] - mean[0], 2);
            sumSquaredDy += Math.pow(point[1] - mean[1], 2);
        }
        return new double[]{ Math.sqrt(sumSquaredDx / data.length), Math.sqrt(sumSquaredDy / data.length) };
    }

    private static String arrayToString(double[] array) {
        return "[" + array[0] + ", " + array[1] + "]";
    }
}