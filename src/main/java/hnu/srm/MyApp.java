package hnu.srm;

import org.opencv.core.Core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class ImageSelectionDialog extends JDialog {

    static String[] imagePaths = {
            "/snake by row right down.PNG",
            "/row by row right down.PNG",
            "/snake by column down right.PNG",
            "/row by row right up.PNG",
            "/column by column down right.PNG",
            "/snake by row left down.PNG",
            "/row by row left down.PNG",
            "/snake by column up right.PNG",
            "/row by row left up.PNG",
            "/column by column down left.PNG"
    };

    public static String getFileNameWithoutExtension(String filePath) {
        // 去掉路径前缀
        String fileNameWithExtension = filePath.substring(1);
        // 找到文件名和后缀的分隔点
        int dotIndex = fileNameWithExtension.lastIndexOf('.');
        // 提取文件名（不包含后缀）
        return fileNameWithExtension.substring(0, dotIndex);
    }

    public ImageSelectionDialog(JFrame parent, Consumer<Integer> onImageSelected) {
        super(parent, "选择拼接路经", true);
        setSize(600, 300);
        setLayout(new BorderLayout());

        JPanel imagePanel = new JPanel(new FlowLayout());
        ButtonGroup buttonGroup = new ButtonGroup();

        // 示例图像路径（替换成实际路径或 BufferedImage）

        JToggleButton[] buttons = new JToggleButton[imagePaths.length];
        for (int i = 0; i < imagePaths.length; i++) {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePaths[i])); // 根目录下

//            ImageIcon icon = new ImageIcon(imagePaths[i]);
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JToggleButton button = new JToggleButton(new ImageIcon(img));
            button.setPreferredSize(new Dimension(100, 100));

            // ⭐ 样式美化
            button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setToolTipText(getFileNameWithoutExtension(imagePaths[i]));

            // ⭐ 鼠标悬停时变暗一点
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(new Color(220, 220, 220));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!button.isSelected()) {
                        button.setBackground(null);
                    }
                }
            });

            // ⭐ 被选中时高亮显示边框
            int finalI = i;
            button.addItemListener(e -> {
                if (button.isSelected()) {
                    button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                    button.setBackground(new Color(200, 220, 255));
                } else {
                    button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
                    button.setBackground(null);
                }
            });

            buttons[i] = button;
            buttonGroup.add(button);
            imagePanel.add(button);
        }


        JButton confirmButton = new JButton("确认选择");
        confirmButton.addActionListener(e -> {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].isSelected()) {
                    onImageSelected.accept(i); // 返回所选编号
                    dispose();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "请选择一张图像！");
        });

        add(imagePanel, BorderLayout.CENTER);
        add(confirmButton, BorderLayout.SOUTH);
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}


public class MyApp {
    static int mode = -1;

    public static void main(String[] args) {
        System.load(new File("opencv_java490.dll").getAbsolutePath());
//        Map<String, String> memoryInfo = getWindowsMemoryInfo();
//        System.out.println("总物理内存: " + parseMemorySize(memoryInfo.get("TotalPhysicalMemory")));
//        System.out.println("可用物理内存: " + memoryInfo.get("AvailablePhysicalMemory"));
//        System.out.println("内存使用率: " + memoryInfo.get("MemoryUsagePercentage") + "%");

        SwingUtilities.invokeLater(() -> {
            new MyApp().createAndShowGUI(); // 非静态方法中用 getClass().getResource(...) 就没问题了
        });
    }

    private long parseMemorySize(String memoryStr) {
        // 从"XX MB"格式的字符串中提取数值部分
        return Long.parseLong(memoryStr.replaceAll("[^0-9]", ""));
    }

    public Map<String, String> getWindowsMemoryInfo() {
        Map<String, String> memoryInfo = new HashMap<>();
        Process process = null;
        BufferedReader reader = null;

        try {
            // 执行wmic命令获取内存信息
            process = Runtime.getRuntime().exec("wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /Format:list");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            long totalMemory = 0;
            long freeMemory = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("TotalVisibleMemorySize=")) {
                    totalMemory = Long.parseLong(line.substring("TotalVisibleMemorySize=".length()));
                } else if (line.startsWith("FreePhysicalMemory=")) {
                    freeMemory = Long.parseLong(line.substring("FreePhysicalMemory=".length()));
                }
            }

            // 转换为MB
            long totalMemoryMB = totalMemory / 1024;
            long freeMemoryMB = freeMemory / 1024;
            long usedMemoryMB = totalMemoryMB - freeMemoryMB;
            double usagePercentage = (double) usedMemoryMB / totalMemoryMB * 100;

            memoryInfo.put("TotalPhysicalMemory", totalMemoryMB + " MB");
            memoryInfo.put("AvailablePhysicalMemory", freeMemoryMB + " MB");
            memoryInfo.put("UsedPhysicalMemory", usedMemoryMB + " MB");
            memoryInfo.put("MemoryUsagePercentage", String.format("%.2f", usagePercentage));

        } catch (IOException | NumberFormatException e) {
            System.err.println("获取内存信息失败: " + e.getMessage());
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return memoryInfo;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("SRM Stitch Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 600);
        frame.setLocationRelativeTo(null); // 居中
        Image icon = Toolkit.getDefaultToolkit().getImage(MyApp.class.getResource("/icon.png"));

        frame.setIconImage(icon);


        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel folderLabel = new JLabel("选择待拼接图像文件夹:");
        JTextField folderField = new JTextField("",20);
        JButton browseButton = new JButton("浏览");

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = chooser.getSelectedFile();
                folderField.setText(selectedFolder.getAbsolutePath());
            }
        });

        JLabel saveFolderLabel = new JLabel("选择保存路径:");
        JTextField saveFolderField = new JTextField("", 20);
        JButton saveButton = new JButton("浏览");

        saveButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = chooser.getSelectedFile();
                saveFolderField.setText(selectedFolder.getAbsolutePath());
            }
        });

        JLabel xLabel = new JLabel("XNums(每列图像数量):");
        JTextField xField = new JTextField("", 5);
        JLabel yLabel = new JLabel("YNums(每行图像数量):");
        JTextField yField = new JTextField("", 5);

        JLabel modeLabel = new JLabel("拼接路经:");
        JButton selectImageButton = new JButton("选择拼接路径");
        JLabel selectedImageIndexLabel = new JLabel("未选择拼接路径");

        selectImageButton.addActionListener(e -> {
            new ImageSelectionDialog(frame, index -> {
                mode = index;
                String fileName = ImageSelectionDialog.getFileNameWithoutExtension(ImageSelectionDialog.imagePaths[index]);
                selectedImageIndexLabel.setText(fileName);
            });
        });

        JLabel rLabel = new JLabel("重叠率:");
        JTextField rField = new JTextField("20", 5);

        JLabel captureLabel = new JLabel("成像模式:");
        JRadioButton option1 = new JRadioButton("明场(更快，占内存更少)");
        JRadioButton option2 = new JRadioButton("其它(更精确，占内存更多)");
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0)); // 左对齐，水平间距 5px
        radioPanel.add(option1);
        radioPanel.add(option2);

        ButtonGroup group = new ButtonGroup();
        group.add(option1);
        group.add(option2);
        option1.setSelected(true);

        JLabel formatLabel = new JLabel("保存格式:");
        JRadioButton format1 = new JRadioButton("tif");
        JRadioButton format2 = new JRadioButton("png");
        JPanel formatPanel = new JPanel();
        formatPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0)); // 左对齐，水平间距 5px
        ButtonGroup group2 = new ButtonGroup();
        group2.add(format1);
        group2.add(format2);
        formatPanel.add(format1);
        formatPanel.add(format2);
        format1.setSelected(true);

        JButton runButton = new JButton("开始拼接");
        JTextArea outputArea = new JTextArea(7, 30);
        outputArea.setEditable(false);

        Map<String, String> memoryInfo = getWindowsMemoryInfo();
        long total = parseMemorySize(memoryInfo.get("TotalPhysicalMemory"));
        long avai = parseMemorySize(memoryInfo.get("AvailablePhysicalMemory"));
        long use = parseMemorySize(memoryInfo.get("MemoryUsagePercentage"));
        outputArea.setText("当前设备总内存" + total + "MB，可用内存" + avai + "MB。");
        if(total < 1024 * 16) {
            outputArea.append("\t当前设备内存过小，建议拼接低分辨率图像");
        }
        if(avai < 1024 * 8) {
            outputArea.append("\t可用内存太少，可能导致拼接失败");
        }


        JLabel ps = new JLabel("");

        runButton.addActionListener(e -> {
            runButton.setEnabled(false); // 禁用按钮，避免重复点击
            outputArea.setText("运行中...\n");
            System.out.println("OpenCV loaded? " + Core.NATIVE_LIBRARY_NAME);
            long start = System.currentTimeMillis();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                String filename = "";
                @Override
                protected Void doInBackground() {
                    try {
                        runButton.setText("拼接中...");
                        String folderPath = folderField.getText();
                        String savePath = saveFolderField.getText();
                        File f = new File(folderPath);
                        if(!f.isDirectory()){
                            outputArea.setText("文件路径异常！");
                            return null;
                        }
                        f = new File(savePath);
                        if(!f.isDirectory()){
                            outputArea.setText("保存路径异常！");
                            return null;
                        }
                        int xNums = Integer.parseInt(xField.getText());
                        int yNums = Integer.parseInt(yField.getText());
                        int r = Integer.parseInt(rField.getText());
                        float lrratio = (float) r / 100;
                        float upratio = (float) r / 100;

                        outputArea.setText("运行中...\n");
                        outputArea.append("拼接目录: " + folderPath + "\n");
                        outputArea.append("保存位置: " + savePath + "\n");

                        boolean orb_dec = option1.isSelected();
                        boolean save_format = format1.isSelected();

                        filename = ImageStitching.process(yNums, xNums, mode, orb_dec, lrratio, upratio, folderPath, savePath, save_format, (current, total) -> SwingUtilities.invokeLater(() -> {
                            if(current < total) {
                                ps.setText("计算中 " + current + "/" + total);
                            }else if(current == total) {
                                String s = ps.getText();
                                ps.setText("计算完成，开始融合...");
                            }else {
                                ps.setText("融合结束，写入磁盘...");
                            }
                        }));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> outputArea.append("运行异常: " + ex.getMessage() + "\n"));
                    }
                    return null;
                }

                @Override
                protected void done() {
                    outputArea.append("运行结束！\n文件名：" + filename + "\ntif格式使用imageJ: Image -> Stacks -> Make Montage... 打开");
                    runButton.setText("开始拼接");
                    long duration = System.currentTimeMillis() - start; // 毫秒

                    long seconds = duration / 1000 % 60;
                    long minutes = duration / (1000 * 60) % 60;
                    long hours   = duration / (1000 * 60 * 60);

                    String readableTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    ps.setText("拼接完成，用时 " + readableTime);
                    runButton.setEnabled(true); // 恢复按钮
                    System.gc();
                }
            };

            worker.execute(); // 启动任务
        });


        gbc.gridx = 0; gbc.gridy = 0; panel.add(folderLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(folderField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; panel.add(browseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(saveFolderLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(saveFolderField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; panel.add(saveButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(xLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(xField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(yLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(yField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(rLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 4; panel.add(rField, gbc);
        gbc.gridx = 0; gbc.gridy = 5; panel.add(modeLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 5; panel.add(selectedImageIndexLabel, gbc);
        gbc.gridx = 2; gbc.gridy = 5; panel.add(selectImageButton, gbc);

        gbc.gridx = 0; gbc.gridy = 6; panel.add(captureLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 6; panel.add(radioPanel, gbc);
//        gbc.gridx = 1; gbc.gridy = 6; panel.add(option1, gbc);
//        gbc.gridx = 2; gbc.gridy = 6; panel.add(option2, gbc);

        gbc.gridx = 0; gbc.gridy = 7; panel.add(formatLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 7; panel.add(formatPanel, gbc);
//        gbc.gridx = 1; gbc.gridy = 7; panel.add(format1, gbc);
//        gbc.gridx = 2; gbc.gridy = 7; panel.add(format2, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3; panel.add(runButton, gbc);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 3; panel.add(ps, gbc);

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 3; panel.add(new JScrollPane(outputArea), gbc);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

    }
}
