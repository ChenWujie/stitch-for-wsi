package hnu.srm;

import java.util.*;

public class DBSCAN {

    public static class Point {
        public final double x, y;
        public boolean visited = false;
        public int cluster = -1; // -1 表示噪声

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 执行 DBSCAN 聚类
     * @param points   输入点集合
     * @param eps      邻域半径
     * @param minPts   邻域最小点数
     * @return 聚类结果（cluster编号 >=0 为有效簇，-1 为噪声）
     */
    public static List<Point> cluster(List<Point> points, double eps, int minPts) {
        int clusterId = 0;
        for (Point p : points) {
            if (!p.visited) {
                p.visited = true;
                List<Point> neighbors = getNeighbors(p, points, eps);
                if (neighbors.size() < minPts) {
                    p.cluster = -1; // 标记为噪声
                } else {
                    expandCluster(p, neighbors, clusterId, eps, minPts, points);
                    clusterId++;
                }
            }
        }
        return points;
    }

    private static void expandCluster(Point p, List<Point> neighbors, int clusterId,
                                      double eps, int minPts, List<Point> points) {
        p.cluster = clusterId;
        Queue<Point> queue = new LinkedList<>(neighbors);
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (!current.visited) {
                current.visited = true;
                List<Point> currentNeighbors = getNeighbors(current, points, eps);
                if (currentNeighbors.size() >= minPts) {
                    queue.addAll(currentNeighbors);
                }
            }
            if (current.cluster == -1) {
                current.cluster = clusterId;
            }
        }
    }

    private static List<Point> getNeighbors(Point p, List<Point> points, double eps) {
        List<Point> neighbors = new ArrayList<>();
        for (Point q : points) {
            if (distance(p, q) <= eps) {
                neighbors.add(q);
            }
        }
        return neighbors;
    }

    private static double distance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
}