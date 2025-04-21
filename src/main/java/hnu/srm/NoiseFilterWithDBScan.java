package hnu.srm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NoiseFilterWithDBScan {

    /**
     * 使用 DBSCAN 剔除异常点后计算均值
     * @param data 位移数据数组，每个元素为 [dx, dy]
     * @param eps  邻域半径（根据数据尺度调整，例如 5.0）
     * @param minPts 最小邻域点数（例如 4）
     * @return 鲁棒均值 [dx, dy]
     */
    public static double[] filterNoiseWithDBSCAN(double[][] data, int minPts) {
        // 转换为 DBSCAN 的点对象
        List<DBSCAN.Point> points = new ArrayList<>();
        for (double[] d : data) {
            points.add(new DBSCAN.Point(d[0], d[1]));
        }

        // 执行聚类
        List<DBSCAN.Point> clustered = DBSCAN.cluster(points, computeAutoEps(data), minPts);

        // 提取所有非噪声点（内点）
        List<double[]> inliers = clustered.stream()
                .filter(p -> p.cluster != -1)
                .map(p -> new double[]{p.x, p.y})
                .collect(Collectors.toList());

        // 如果没有内点，返回全局均值
        if (inliers.isEmpty()) {
            return calculateMean(data);
        }
        // 计算内点均值
        return calculateMean(inliers.toArray(new double[0][]));
    }

    private static double[] calculateMean(double[][] data) {
        double sumX = 0, sumY = 0;
        for (double[] d : data) {
            sumX += d[0];
            sumY += d[1];
        }
        return new double[]{sumX / data.length, sumY / data.length};
    }

    static double computeAutoEps(double[][] data) {
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            double minDist = Double.MAX_VALUE;
            for (int j = 0; j < data.length; j++) {
                if (i != j) {
                    double dist = Math.sqrt(
                            Math.pow(data[i][0] - data[j][0], 2) +
                                    Math.pow(data[i][1] - data[j][1], 2)
                    );
                    if (dist < minDist) minDist = dist;
                }
            }
            distances.add(minDist);
        }
        Collections.sort(distances);
        return distances.get(distances.size() / 2); // 中位数
    }
}