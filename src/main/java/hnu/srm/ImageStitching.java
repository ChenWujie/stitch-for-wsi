package hnu.srm;


import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;




public class ImageStitching {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    static boolean orb_dec = true;
    static double global_rmse = 0;
    static int rmse_num = 0;

    static int progress = 0;

    // 计算修正后图像之间的位移距离
    public static MyPosition CalculateOffset(Size size, int overlap, int dx, int dy, boolean lr) {

        // 上下位置： 在下方dy<0，在上方dy>0； 在右方dx<0，在左方dx>0
        int width = (int) size.width, height = (int) size.height;
        int initX, initY;
        if (lr) {   //lr为true：相对位置左右
            initX = width - overlap;
            initY = 0;
        }else {     //lr为false：相对位置上下
            initX = 0;
            initY = height - overlap;
        }
        initX -= dx;
        initY -= dy;

        return new MyPosition(initX, initY);    //返回坐标为将两幅图像重叠在一起后的相对位移坐标
    }

    /*
    传入图像尺寸和相对位移、拼接方式
    返回A、B的ROI区域以及拼接后的图像尺寸
     */
    public static Rect[] CalculateRect(Size size, MyPosition startPosition, boolean lr) {
        Rect[] rects = new Rect[3];
        if (lr) {
            if(startPosition.y > 0) {
                rects[0] = new Rect(0, 0, (int) size.width, (int) size.height);
                rects[1] = new Rect(startPosition.x, startPosition.y, (int) size.width, (int) size.height);
            }else {
                rects[0] = new Rect(0, -1 * startPosition.y, (int) size.width, (int) size.height);
                rects[1] = new Rect(startPosition.x, 0, (int) size.width, (int) size.height);
            }
            rects[2] = new Rect(0, 0, startPosition.x + (int) size.width, (int) size.height + Math.abs(startPosition.y));
        }else {
            if (startPosition.x > 0) {
                rects[0] = new Rect(0, 0, (int) size.width, (int) size.height);
                rects[1] = new Rect(startPosition.x, startPosition.y, (int) size.width, (int) size.height);
            }else {
                rects[0] = new Rect(-1 * startPosition.x, 0, (int) size.width, (int) size.height);
                rects[1] = new Rect(0, startPosition.y, (int) size.width, (int) size.height);
            }
            rects[2] = new Rect(0, 0, Math.abs(startPosition.x) + (int) size.width, (int) size.height + startPosition.y);
        }
        //0: A的区域； 1：B的区域； 2：拼接图像的尺寸
        return rects;
    }

    //返回拼接结果
    public static Mat Stitch(String pathA, String pathB, float ratio, boolean lr) {
        Mat imgA = Imgcodecs.imread(pathA);
        Mat imgB = Imgcodecs.imread(pathB);
        // 检查图片是否加载成功
        if (imgA.empty()) {
            System.out.println("图片A加载失败！路径: " + pathA);
            System.exit(0);
        }
        if (imgB.empty()) {
            System.out.println("图片B加载失败！路径: " + pathB);
            System.exit(0);
        }

        //调用此函数，传入图像路径，返回相对位移
        PositionAndWeight positionAndWeight = Match(pathA, pathB, ratio, lr);
        MyPosition startPosition = positionAndWeight.myPosition;

        //根据相对位移分别计算 A、B的ROI区域以及最终的拼接尺寸
        Rect[] rects = CalculateRect(imgA.size(), startPosition, lr);
        //根据偏移距离和拼接方式，计算最终的拼接结果
        System.out.println(startPosition);
        // 创建拼接结果图像`
        Mat result = new Mat(rects[2].height, rects[2].width, imgA.type(), Scalar.all(0));

        // 将imgA复制到result中
        Mat roiAResult = result.submat(rects[0]);
        imgA.copyTo(roiAResult);

        Mat roiBResult = result.submat(rects[1]);
        imgB.copyTo(roiBResult);
        return result;
    }

    // 计算图像的平均亮度
    private static double calculateAverageBrightness(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Scalar meanScalar = Core.mean(gray);
        return meanScalar.val[0];
    }

    public static PositionAndWeight Match(String pathA, String pathB, float ratio, boolean lr) {
        // 读取图片
        Mat imgA = Imgcodecs.imread(pathA);
        Mat imgB = Imgcodecs.imread(pathB);
        // 检查图片是否加载成功
        if (imgA.empty()) {
            System.out.println("图片A加载失败！路径: " + pathA);
            System.exit(0);
        }
        if (imgB.empty()) {
            System.out.println("图片B加载失败！路径: " + pathB);
            System.exit(0);
        }

//        if(!brightness.containsKey(pathA)) {
//            brightness.put(pathA, calculateAverageBrightness(imgA));
//        }
//        if(!brightness.containsKey(pathB)) {
//            brightness.put(pathB, calculateAverageBrightness(imgB));
//        }

        Mat imgACrop, imgBCrop;
        // 计算重叠部分的宽度（假设重叠部分为10%）
        int overlap;
        if (lr) {
            // 裁剪出图像的重叠部分:左右
            overlap = (int) (imgA.cols() * ratio);
            imgACrop = new Mat(imgA, new Rect(imgA.cols() - overlap, 0, overlap, imgA.rows()));
            imgBCrop = new Mat(imgB, new Rect(0, 0, overlap, imgB.rows()));
        }else {
            overlap = (int) (imgA.rows() * ratio);
            //上下
            imgACrop = new Mat(imgA, new Rect(0, imgA.rows() - overlap, imgA.cols(), overlap));
            imgBCrop = new Mat(imgB, new Rect(0, 0, imgB.cols(), overlap));
        }

        List<MatOfDMatch> knnMatches = new ArrayList<>();
        MatOfKeyPoint keypointsA = new MatOfKeyPoint();
        MatOfKeyPoint keypointsB = new MatOfKeyPoint();
        Mat descriptorsA = new Mat();
        Mat descriptorsB = new Mat();
        // ORB特征检测器和描述符
        if(orb_dec) {
            ORB orb = ORB.create();

            // 检测特征点并计算描述符
            try {
                orb.detectAndCompute(imgACrop, new Mat(), keypointsA, descriptorsA);
                orb.detectAndCompute(imgBCrop, new Mat(), keypointsB, descriptorsB);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("特征检测和描述符计算失败！");
                return new PositionAndWeight(new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE), 1, 0);
            }

            if(keypointsA.size().height < 10 || keypointsB.size().height < 10) {
                return new PositionAndWeight(new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE), 1, 0);
            }
            // 特征匹配
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            matcher.knnMatch(descriptorsA, descriptorsB, knnMatches, 2);
        }else {
            //SIFT特征检测器和描述符
            SIFT sift = SIFT.create();

// 关键点和描述符


// 检测特征点并计算描述符
            try {
                sift.detectAndCompute(imgACrop, new Mat(), keypointsA, descriptorsA);
                sift.detectAndCompute(imgBCrop, new Mat(), keypointsB, descriptorsB);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("SIFT特征检测失败！");
                return new PositionAndWeight(new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE), 1, 0);
            }

            if (keypointsA.size().height < 10 || keypointsB.size().height < 10) {
                return new PositionAndWeight(new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE), 1, 0);
            }

// 使用FLANN匹配器（需L2距离）
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
            matcher.knnMatch(descriptorsA, descriptorsB, knnMatches, 2);
        }

        // 筛选出好的匹配
        float ratioThresh = 0.75f;
        List<DMatch> goodMatches = new LinkedList<>();
        for (MatOfDMatch matOfDMatch : knnMatches) {
            DMatch[] matches = matOfDMatch.toArray();
            if (matches[0].distance < ratioThresh * matches[1].distance) {
                goodMatches.add(matches[0]);
            }
        }

        // 画出匹配结果
//        MatOfDMatch goodMatchesMat = new MatOfDMatch();
//        goodMatchesMat.fromList(goodMatches);
//        Mat imgMatches = new Mat();
//        Features2d.drawMatches(imgACrop, keypointsA, imgBCrop, keypointsB, goodMatchesMat, imgMatches);
//        // 保存图像
//        String outputPath = "matches_result.png"; // 保存的文件路径和文件名
//        boolean saved = Imgcodecs.imwrite(outputPath, imgMatches);
//        if (saved) {
//            System.out.println("图像保存成功，路径为: " + outputPath);
//        } else {
//            System.err.println("图像保存失败。");
//        }
//        HighGui.imshow("Matches", imgMatches);
//        HighGui.waitKey();
//        System.exit(-1);

        // 如果匹配点数不足，直接返回
        if (goodMatches.size() < 4) {
            System.out.println("匹配点数不足，无法拼接图像");
            return new PositionAndWeight(new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE), 1, 0);
        }

        //        // 从匹配中提取关键点的位置
//        List<Point> pointsA = new ArrayList<>();
//        List<Point> pointsB = new ArrayList<>();
//        KeyPoint[] keypointsAArray = keypointsA.toArray();
//        KeyPoint[] keypointsBArray = keypointsB.toArray();
//        for (DMatch match : goodMatches) {
//            // 注意，关键点的位置需要加上裁剪时的偏移量
//            Point ptA = keypointsAArray[match.queryIdx].pt;
//            ptA.x += imgA.cols() - overlapWidthA; // 加上偏移量
//            pointsA.add(ptA);
//
//            Point ptB = keypointsBArray[match.trainIdx].pt;
//            pointsB.add(ptB);
//        }
//
//        // 计算单应矩阵
//        MatOfPoint2f pointsAMat = new MatOfPoint2f();
//        pointsAMat.fromList(pointsA);
//        MatOfPoint2f pointsBMat = new MatOfPoint2f();
//        pointsBMat.fromList(pointsB);
//        Mat homography = Calib3d.findHomography(pointsBMat, pointsAMat, Calib3d.RANSAC, 5);
//
//        // 拼接图像
//        Mat result = new Mat();
//        Imgproc.warpPerspective(imgB, result, homography, new Size(imgA.cols() + imgB.cols(), imgA.rows()));
//        Mat half = new Mat(result, new Rect(0, 0, imgA.cols(), imgA.rows()));
//        imgA.copyTo(half);
//
//        // 显示拼接结果
//        HighGui.imshow("Result", result);
//        HighGui.waitKey();

        // 从匹配中提取关键点的位置并计算位移
        List<Point> pointsA = new ArrayList<>();
        List<Point> pointsB = new ArrayList<>();
        KeyPoint[] keypointsAArray = keypointsA.toArray();
        KeyPoint[] keypointsBArray = keypointsB.toArray();
        for (DMatch match : goodMatches) {
            pointsA.add(keypointsAArray[match.queryIdx].pt);
            pointsB.add(keypointsBArray[match.trainIdx].pt);
        }

        // 计算平均位移
        double sumDx = 0;
        double sumDy = 0;
        int numPoints = pointsA.size();
        double[][] data = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            sumDx += pointsB.get(i).x - pointsA.get(i).x;
            sumDy += pointsB.get(i).y - pointsA.get(i).y;
            data[i][0] = pointsB.get(i).x - pointsA.get(i).x;
            data[i][1] = pointsB.get(i).y - pointsA.get(i).y;
        }

//        double[] offset = NoiseFilter.filterNoiseAndCalculateMean(data);
        double[] offset = NoiseFilterWithDBScan.filterNoiseWithDBSCAN(data, 3);
        // 在计算 offset 后添加以下代码：

// data 是存储所有匹配点位移差的数组（dx, dy）
        double sumSquaredError = 0.0;
        for (int i = 0; i < data.length; i++) {
            double dx = data[i][0];
            double dy = data[i][1];
            // 计算每个点的位移差与平均位移的误差
            double errorX = dx - offset[0];
            double errorY = dy - offset[1];
            sumSquaredError += (errorX * errorX + errorY * errorY);
        }
        double rmse = Math.sqrt(sumSquaredError / data.length);

        double avgDx = sumDx / numPoints;
        double avgDy = sumDy / numPoints;
//        System.out.println("直接求取均值： " + avgDx + "  " + avgDy);
//        System.out.println("剔除误差后均值： " + offset[0] + "  " + offset[1]);
        PositionAndWeight positionAndWeight = new PositionAndWeight(CalculateOffset(imgA.size(), overlap, (int) offset[0], (int) offset[1], lr), (int) 100 * goodMatches.size() / knnMatches.size(), rmse);
        imgB.release();
        imgACrop.release();
        imgA.release();
        imgBCrop.release();
        return positionAndWeight;
    }

    public static String[][] GetFileNames(String path, int xNums, int yNums, int mode) {
        File f = new File(path);
        if(!f.isDirectory()){
            System.out.println("路径不是文件夹！");
            System.exit(1);
        }
        String[] fileList = f.list();
        /*
        \  0: snake by row down
         */
        String[][] fileNames = new String[yNums][xNums];
        switch (mode) {
            case 0:
                //snake by row
                for(int r = 0; r < yNums; r++) {
                    for(int c = 0; c < xNums; c++) {
                        if(r % 2 == 0)
                            fileNames[r][c] = path + "/" + fileList[r * xNums + c];
                        else
                            fileNames[r][c] = path + "/" + fileList[(r + 1) * xNums - 1 - c];
                    }
                }
                break;
            case 1:
                //row by row down
                for(int r = 0; r < yNums; r++) {
                    for(int c = 0; c < xNums; c++) {
                        fileNames[r][c] = path + "/" + fileList[r * xNums + c];
                    }
                }
                break;
            case 2:
                //snake by column
                for(int c = 0; c < xNums; c++) {
                    for(int r = 0; r < yNums; r++) {
                        if(c % 2 == 0)
                            fileNames[r][c] = path + "/" + fileList[c * yNums + r];
                        else
                            fileNames[r][c] = path + "/" + fileList[(c + 1) * yNums - 1 - r];
                    }
                }
                break;
            case 3:
                //row by row up
                for(int r = 0; r < yNums; r++) {
                    for(int c = 0; c < xNums; c++) {
                        fileNames[r][c] = path + "/" + fileList[(yNums-r-1)*xNums + c];
                    }
                }
                break;
            case 4:
                //column by column right
                for(int r = 0; r < yNums; r++) {
                    for(int c = 0; c < xNums; c++) {
                        fileNames[r][c] = path + "/" + fileList[c*yNums+r];
                    }
                }
                break;
        }
        return fileNames;
    }

    public static OffsetsAndWeights CalculateAllOffset(String path, int xNums, int yNums, float lrratio, float upratio, int mode, ProgressListener listener) {
        String[][] fileNames = GetFileNames(path, xNums, yNums, mode);

        MyPosition[][][] offsets = new MyPosition[yNums][xNums][2];
        int[][][] weights = new int[yNums][xNums][2];
        for(int r = 0; r < yNums - 1; r++) {
            for(int c = 0; c < xNums - 1; c++) {
                progress++;
                System.out.println("计算第"+(r*xNums+c)+"张图像特征。。。");
                // 每处理完一张图，通知监听器
                if (listener != null) {
                    listener.onProgress(progress, xNums*yNums);
                }
                PositionAndWeight p1 = Match(fileNames[r][c], fileNames[r][c+1], lrratio, true);
                offsets[r][c][0] = p1.myPosition;
                weights[r][c][0] = p1.weight;
                if(p1.rmse>0 && p1.rmse<10) {
                    global_rmse += p1.rmse;
                    rmse_num += 1;
                }
                PositionAndWeight p2 = Match(fileNames[r][c], fileNames[r+1][c], upratio, false);
                offsets[r][c][1] = p2.myPosition;
                weights[r][c][1] = p2.weight;
                if(p2.rmse>0 && p2.rmse<10) {
                    global_rmse += p2.rmse;
                    rmse_num += 1;
                }
            }
        }
        //最后一行
        for(int c = 0; c < xNums-1; c++) {
            progress++;
            if (listener != null) {
                listener.onProgress(progress, xNums*yNums);
            }
            PositionAndWeight p2 = Match(fileNames[yNums-1][c], fileNames[yNums-1][c+1], lrratio, true);
            offsets[yNums-1][c][0] = p2.myPosition;
            weights[yNums-1][c][0] = p2.weight;
            offsets[yNums-1][c][1] = null;
            weights[yNums-1][c][1] = -1;
            if(p2.rmse>0 && p2.rmse<10) {
                global_rmse += p2.rmse;
                rmse_num += 1;
            }
        }
        //最后一列
        for(int r = 0; r < yNums-1; r++) {
            progress++;
            if (listener != null) {
                listener.onProgress(progress, xNums*yNums);
            }
            PositionAndWeight p2 = Match(fileNames[r][xNums-1], fileNames[r+1][xNums-1], upratio, false);
            offsets[r][xNums-1][1] = p2.myPosition;
            weights[r][xNums-1][1] = p2.weight;
            offsets[r][xNums-1][0] = null;
            weights[r][xNums-1][0] = -1;
            if(p2.rmse>0 && p2.rmse<10) {
                global_rmse += p2.rmse;
                rmse_num += 1;
            }
        }
        //最后一个
        offsets[yNums-1][xNums-1][0] = null;
        offsets[yNums-1][xNums-1][1] = null;
        weights[yNums-1][xNums-1][0] = -1;
        weights[yNums-1][xNums-1][1] = -1;
        progress++;
        if (listener != null) {
            listener.onProgress(progress, xNums*yNums);
        }

        //处理匹配失败的图像对
        MyPosition[] meanPositionH = new MyPosition[yNums];
        boolean flag=false;
        for(int r = 0; r < yNums; r++) {
            int n = 0, sumX=0, sumY=0;
            for(int c = 0; c < xNums - 1; c++) {
                if(offsets[r][c][0].x < Integer.MAX_VALUE) {
                    sumX += offsets[r][c][0].x;
                    sumY += offsets[r][c][0].y;
                    n++;
                }
            }
            if(n==0) {
                meanPositionH[r] = new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE);
                flag = true;
            }
            else {
                meanPositionH[r] = new MyPosition(sumX / n, sumY / n);
            }
        }
        if(flag) {
            int temp=0, sumX=0, sumY=0;
            for(int r = 0; r < yNums; r++) {
                if(meanPositionH[r].x < Integer.MAX_VALUE) {
                    sumX += meanPositionH[r].x;
                    sumY += meanPositionH[r].y;
                    temp += 1;
                }
            }
            for(int r = 0; r < yNums; r++) {
                if(meanPositionH[r].x == Integer.MAX_VALUE && temp != 0){
                    meanPositionH[r] = new MyPosition(sumX /temp, sumY / temp);
                }
            }
        }

        flag = false;
        MyPosition[] meanPositionV = new MyPosition[xNums];
        for(int c = 0; c < xNums; c++) {
            int n = 0, sumX=0, sumY=0;
            for(int r = 0; r < yNums - 1; r++) {
                if(offsets[r][c][1].y < Integer.MAX_VALUE) {
                    sumX += offsets[r][c][1].x;
                    sumY += offsets[r][c][1].y;
                    n++;
                }
            }
            if(n==0) {
                meanPositionV[c] = new MyPosition(Integer.MAX_VALUE, Integer.MAX_VALUE);
                flag = true;
            }
            else{
                meanPositionV[c] = new MyPosition(sumX / n, sumY / n);
            }
        }
        if(flag) {
            int temp=0, sumX=0, sumY=0;
            for(int c = 0; c < xNums; c++) {
                if(meanPositionV[c].x < Integer.MAX_VALUE) {
                    sumX += meanPositionV[c].x;
                    sumY += meanPositionV[c].y;
                    temp += 1;
                }
            }
            for(int c = 0; c < xNums; c++) {
                if(meanPositionV[c].x == Integer.MAX_VALUE && temp != 0){
                    meanPositionV[c] = new MyPosition(sumX /temp, sumY / temp);
                }
            }
        }

        for(int r = 0; r < yNums; r++) {
            for(int c = 0; c < xNums; c++) {
                if(offsets[r][c][0] != null && offsets[r][c][0].x == Integer.MAX_VALUE) {
                    offsets[r][c][0] = meanPositionH[r];
                }
                if(offsets[r][c][1] != null && offsets[r][c][1].y == Integer.MAX_VALUE) {
                    offsets[r][c][1] = meanPositionV[c];
                }
            }
        }


        System.out.println("修正位移完成");
        OffsetsAndWeights offsetsAndWeights = new OffsetsAndWeights(offsets, weights, fileNames);
        System.out.println(offsetsAndWeights.toString());
        return offsetsAndWeights;
    }

    public static List<PrimMaxSpanningTreeGUI.Edge> CalculateRoad(OffsetsAndWeights offsetsAndWeights) {
        MyPosition[][][] offsets = offsetsAndWeights.offsets;
        int[][][] weights = offsetsAndWeights.weights;

        int yNums=offsets.length, xNums=offsets[0].length;

//        Map<Integer, List<PrimMaxSpanningTree.Edge>> graph = new HashMap<>();
//
//        //graph.computeIfAbsent(0, k -> new ArrayList<>()).add(new Edge(1, 4));
//        for(int r = 0; r < yNums; r++) {
//            for(int c = 0; c < xNums; c++) {
//                int v = r * xNums + c;
//                if(offsets[r][c][0] != null) {
//                    graph.computeIfAbsent(v, k -> new ArrayList<>()).add(new PrimMaxSpanningTree.Edge(v+1, weights[r][c][0]));
//                    graph.computeIfAbsent(v+1, k -> new ArrayList<>()).add(new PrimMaxSpanningTree.Edge(v, weights[r][c][0]));
//                }
//                if(offsets[r][c][1] != null) {
//                    graph.computeIfAbsent(v, k -> new ArrayList<>()).add(new PrimMaxSpanningTree.Edge(v+offsets[0].length, weights[r][c][1]));
//                    graph.computeIfAbsent(v+offsets[0].length, k -> new ArrayList<>()).add(new PrimMaxSpanningTree.Edge(v, weights[r][c][1]));
//                }
//            }
//        }
//        int startNode = 0;
//        List<PrimMaxSpanningTree.Edge> maxTree = PrimMaxSpanningTree.primMaxSpanningTree(graph, startNode);
//        System.out.println("最大生成树的边：");
//        // 按 vertex 排序
//        Collections.sort(maxTree, new Comparator<PrimMaxSpanningTree.Edge>() {
//            @Override
//            public int compare(PrimMaxSpanningTree.Edge e1, PrimMaxSpanningTree.Edge e2) {
//                return Integer.compare(e1.vertex, e2.vertex);
//            }
//        });
//        for (PrimMaxSpanningTree.Edge edge : maxTree) {
//            System.out.println("顶点: " + edge.vertex + ", 权重: " + edge.weight);
//        }
        JFrame frame = new JFrame("Prim's Maximum Spanning Tree");
        List<PrimMaxSpanningTreeGUI.Edge> edges = new ArrayList<>();
        // Add edges to the list
        for(int r = 0; r < yNums; r++) {
            for (int c = 0; c < xNums; c++) {
                int v = r * xNums + c;
                if (offsets[r][c][0] != null) {
                    edges.add(new PrimMaxSpanningTreeGUI.Edge(v, v+1, weights[r][c][0]));
                }
                if (offsets[r][c][1] != null) {
                    edges.add(new PrimMaxSpanningTreeGUI.Edge(v, v+xNums, weights[r][c][1]));
                }
            }
        }

//        JFrame f = new JFrame("All");
//        PrimMaxSpanningTreeGUI panel1 = new PrimMaxSpanningTreeGUI(edges, xNums, yNums);
//        f.add(panel1);
//        f.setSize(1920, 1080);
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        f.setVisible(true);


        List<PrimMaxSpanningTreeGUI.Edge> maxTree = PrimMaxSpanningTreeGUI.primMaxSpanningTree(xNums * yNums, edges);

        //画出最大生成树
//        PrimMaxSpanningTreeGUI panel = new PrimMaxSpanningTreeGUI(maxTree, xNums, yNums);
//        frame.add(panel);
//        frame.setSize(1920, 1080);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setVisible(true);
        return maxTree;
    }

    public static MyPosition[][] adjustOffsets(MyPosition[][][] offsets, List<PrimMaxSpanningTreeGUI.Edge> maxTree, int height, int width) {
        int yNums=offsets.length, xNums = offsets[0].length;
        MyPosition[][] abosluteOffset = new MyPosition[yNums][xNums];
        boolean[][] flags = new boolean[yNums][xNums];
        abosluteOffset[0][0] = new MyPosition(0, 0);
        flags[0][0] = true;
        int resultHeight = height, resultWidth = width;

        for(PrimMaxSpanningTreeGUI.Edge edge : maxTree) {
            int row=edge.vertex1 / xNums, col=edge.vertex1%xNums;
            //计算相对位置
            int difference = edge.vertex2 - edge.vertex1;
            MyPosition offset;
            if(difference==1) {
                offset = offsets[row][col][0];
            }else if(difference==xNums) {
                offset = offsets[row][col][1];
            }else if(difference==-1) {
                offset = offsets[row][col-1][0].reverse();
            }else{
                offset = offsets[row-1][col][1].reverse();
            }
            MyPosition newPosition = MyPosition.add(abosluteOffset[row][col], offset);
            if(newPosition.x<0) {
                for(int r=0; r<yNums; r++) {
                    for(int c=0; c<xNums; c++) {
                        if(flags[r][c]){
                            abosluteOffset[r][c].x += Math.abs(newPosition.x);
                        }
                    }
                }
                newPosition.x = 0;
            }
            if(newPosition.y<0) {
                for(int r=0; r<yNums; r++) {
                    for(int c=0; c<xNums; c++) {
                        if(flags[r][c]){
                            abosluteOffset[r][c].y += Math.abs(newPosition.y);
                        }
                    }
                }
                newPosition.y = 0;
            }
            abosluteOffset[edge.vertex2/xNums][edge.vertex2%xNums]=newPosition;
            flags[edge.vertex2/xNums][edge.vertex2%xNums]=true;
        }
        return abosluteOffset;
    }

    public static Mat Fusion2(Mat matA, Mat matB) {
//        long start = System.currentTimeMillis();

        Mat mA = new Mat();

        if(matA.channels() > 1)
            Imgproc.cvtColor(matA, mA, Imgproc.COLOR_BGR2GRAY);
        else
            mA = matA.clone();

        Mat mask = new Mat();
        Core.compare(mA, new Scalar(0), mask, Core.CMP_EQ);
        mA.release();
        // 将掩膜转换为浮点型
        Mat maskFloatB = new Mat();
        mask.convertTo(maskFloatB, CvType.CV_32F, 1.0 / 255.0);

        Mat maskFloatA = new Mat();
        Core.subtract(Mat.ones(maskFloatB.size(), maskFloatB.type()), maskFloatB, maskFloatA);

        // 创建用于存储距离变换结果的Mat
        Mat dist = new Mat();
        // 将mat转换为8位无符号类型
        //Mat mat8U = new Mat();

        Mat mau = new Mat();
        maskFloatA.convertTo(mau, CvType.CV_8UC1, 255);  // 乘以255将浮点型值转换为8位无符号值
        // 计算距离变换 非0区域到0的距离
        Imgproc.distanceTransform(mau, dist, Imgproc.DIST_L2, 3);
        mask.release();
        mau.release();

        // 归一化距离值到0到1之间
        Core.normalize(dist, dist, 0, 1, Core.NORM_MINMAX);
//        System.out.print("开始");
        // 在Mat的0区域应用平滑过渡
//        for (int a = 0; a < maskFloatA.rows(); a++) {
//            for (int j = 0; j < maskFloatA.cols(); j++) {
//                double value = maskFloatA.get(a, j)[0];
//                if (value != 0) {
//                    maskFloatA.put(a, j, dist.get(a, j));
//                }
//            }
//        }
        Core.multiply(maskFloatA, dist, maskFloatA);
        dist.release();
        Core.subtract(Mat.ones(maskFloatB.size(), maskFloatA.type()), maskFloatA, maskFloatB);
//        for (int a = 0; a < maskFloatB.rows(); a ++) {
//            for ( int j = 0; j < maskFloatB.cols(); j ++) {
//                double value = maskFloatB.get(a, j)[0];
//                if (value == 0) {
//                    maskFloatB.put(a, j, 1-maskFloatA.get(a, j)[0]);
//                }
//            }
//        }

//        System.out.println(" 用时：" + (System.currentTimeMillis()-start) + "毫秒");
        // 分离三通道图像的通道
        List<Mat> channelsA = new ArrayList<>();
        List<Mat> channelsB = new ArrayList<>();
        Core.split(matA, channelsA);
        Core.split(matB, channelsB);

        List<Mat> resultChannels = new ArrayList<>();


        // 对每个通道进行逐元素乘法运算
        for (int i = 0; i < channelsA.size(); i++) {
                        // 转换到浮点型
            Mat channelAFloat = new Mat();
            Mat channelBFloat = new Mat();
            channelsA.get(i).convertTo(channelAFloat, CvType.CV_32F);
            channelsB.get(i).convertTo(channelBFloat, CvType.CV_32F);
            Core.multiply(channelAFloat, maskFloatA, channelAFloat);
            Core.multiply(channelBFloat, maskFloatB, channelBFloat);
            Mat resultChannel = new Mat();
            Core.add(channelAFloat, channelBFloat, resultChannel);
            resultChannels.add(resultChannel);
        }

        Mat result = new Mat();
        if(resultChannels.size()>1)Core.merge(resultChannels, result);
        else result= resultChannels.get(0);
        // 将结果转换回8位图像
//        Mat finalResult = new Mat();

//        result.convertTo(result, CvType.CV_8UC3);
        result.convertTo(result, matA.type());

        return result;
    }

    public static Mat StitchWithAbsPosition(MyPosition[][] absPosition, String[][] fileNames, int xNums, int yNums, boolean fuse) {
        int maxHeight=0, maxWidth=0;
        Mat imgA = Imgcodecs.imread(fileNames[0][0], Imgcodecs.IMREAD_UNCHANGED);
        int rows=imgA.rows(), cols=imgA.cols();
        for(int r = 0; r < yNums; r++ ) {
            for(int c = 0; c < xNums; c++ ) {
                maxWidth = Math.max(absPosition[r][c].x + cols, maxWidth);
                maxHeight = Math.max(absPosition[r][c].y + rows, maxHeight);
            }
        }
        //创建拼接后最终大小的矩阵
        System.out.println("maxHeight: " + maxHeight + "  " + maxWidth);
        if(maxHeight>21474836 || maxWidth>21474836){
            System.out.println("特征点不足以拼接，程序退出");
            System.exit(1);
        }
        Mat result = new Mat(maxHeight, maxWidth, imgA.type(), Scalar.all(0));
        imgA.release();

        //遍历填入图像
        for(int r = 0; r < yNums; r++ ) {
            for(int c = 0; c < xNums; c++ ) {
                Mat image = Imgcodecs.imread(fileNames[r][c], Imgcodecs.IMREAD_UNCHANGED);
                Rect rect = new Rect(absPosition[r][c].x, absPosition[r][c].y, image.cols(), image.rows());
                Mat roi = result.submat(rect);
                if(fuse) image = Fusion2(roi, image);
                image.copyTo(roi);
//                image = null;
                image.release();
                System.gc();
            }
//            System.out.print("\n");

        }
//        System.out.println("均值：" + targetBrightness);
        return result;
    }

    public static String process(int xNums, int yNums, int mode, boolean o, float lrratio, float upratio, String path, String save, boolean tif, ProgressListener listener) throws Exception {
        progress = 0;
        orb_dec = o;
        OffsetsAndWeights offsetsAndWeights = CalculateAllOffset(path, xNums, yNums, lrratio, upratio, mode, listener);
        List<PrimMaxSpanningTreeGUI.Edge> maxTree = CalculateRoad(offsetsAndWeights);
        MyPosition[][] absPosition = adjustOffsets(offsetsAndWeights.offsets, maxTree, yNums, xNums);
        boolean fusion = true;
        System.out.println("开始融合。。。");
        System.gc();
        Mat result = StitchWithAbsPosition(absPosition, offsetsAndWeights.fileNames, xNums, yNums, fusion);
//        String filename = save + "\\result_" + path.substring(path.length()-4) + ".png";
//        Imgcodecs.imwrite(filename, result);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = save + "\\" + timestamp;
        if(tif) {
            filename += ".tif";
        }else {
            filename += ".png";
        }

        // 1. 确保路径存在
        File parent = new File(filename).getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

// 2. 检查图像是否为空
        if (result != null && !result.empty()) {
            System.out.println("开始保存" + filename);
            progress++;
            if (listener != null) {
                listener.onProgress(progress, xNums*yNums);
            }
            if(tif) {
                TiffSaver.saveMatInRowChunks(result, new File(filename), 2000);
                System.out.println(result.size());
                System.out.println("拼接结束，保存在" + filename);
            }else {
                {
                    boolean success = Imgcodecs.imwrite(filename, result);
                    if (!success) {
                        System.err.println("❌ 图像保存失败！");
                    } else {
                        System.out.println("✅ 成功保存到: " + filename);
                    }
                }
            }
            result.release();
        } else {
            System.err.println("⚠️ 图像为空，无法保存！");
        }
        return filename;
    }
}
