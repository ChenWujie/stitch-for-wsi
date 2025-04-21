package hnu.srm;

import org.opencv.core.Core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.function.Consumer;

class ImageSelectionDialog extends JDialog {

    static String[] imagePaths = {
            "/snake by row.PNG",
            "/row by row down.PNG",
            "/snake by column.PNG",
            "/row by row up.PNG",
            "/column by column.PNG"
    };

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
            button.setToolTipText("点击选择第 " + (i + 1) + " 张图像");

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

        SwingUtilities.invokeLater(() -> {
            new MyApp().createAndShowGUI(); // 非静态方法中用 getClass().getResource(...) 就没问题了
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("SRM Stitch Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 550);
        frame.setLocationRelativeTo(null); // 居中
        Image icon = Toolkit.getDefaultToolkit().getImage(MyApp.class.getResource("/icon.png"));


        frame.setIconImage(icon);


        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel folderLabel = new JLabel("选择待拼接图像文件夹:");
        JTextField folderField = new JTextField(20);
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
        JTextField saveFolderField = new JTextField(20);
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

        JLabel xLabel = new JLabel("XNums:");
        JTextField xField = new JTextField("10", 5);
        JLabel yLabel = new JLabel("YNums:");
        JTextField yField = new JTextField("10", 5);

        JLabel modeLabel = new JLabel("拼接路经:");
        JButton selectImageButton = new JButton("选择拼接路径");
        JLabel selectedImageIndexLabel = new JLabel("未选择拼接路径");

        selectImageButton.addActionListener(e -> {
            new ImageSelectionDialog(frame, index -> {
                mode = index;
                String fileName = Paths.get(ImageSelectionDialog.imagePaths[index]).getFileName().toString(); // "row by row.png"
                String nameWithoutExtension = fileName.replaceFirst("[.][^.]+$", ""); // 去除扩展名
                selectedImageIndexLabel.setText(nameWithoutExtension);
            });
        });

        JLabel rLabel = new JLabel("重叠率:");
        JTextField rField = new JTextField("20", 5);

        JRadioButton option1 = new JRadioButton("明场");
        JRadioButton option2 = new JRadioButton("其它");


// 创建 ButtonGroup，把所有按钮放进去
        ButtonGroup group = new ButtonGroup();
        group.add(option1);
        group.add(option2);

// 设置默认选中
        option1.setSelected(true);


        JButton runButton = new JButton("开始拼接");
        JTextArea outputArea = new JTextArea(6, 30);
        outputArea.setEditable(false);

        JLabel ps = new JLabel("");

        runButton.addActionListener(e -> {
            runButton.setEnabled(false); // 禁用按钮，避免重复点击
            outputArea.setText("运行中...\n");
            System.out.println("OpenCV loaded? " + Core.NATIVE_LIBRARY_NAME);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
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
                        outputArea.append("拼接的目录: " + folderPath + "\n");
                        outputArea.append("保存的目录: " + savePath + "\n");
                        outputArea.append("参数: x=" + xNums + ", y=" + yNums + ", r=" + r + "\n");

                        ImageStitching.process(xNums, yNums, mode, lrratio, upratio, folderPath, savePath, (current, total) -> SwingUtilities.invokeLater(() -> {
                            ps.setText("计算中 " + current + "/" + total);
                            if(current == total) {
                                String s = ps.getText();
                                ps.setText("计算完成，开始融合...");
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
                    outputArea.append("运行结束！\n");
                    runButton.setText("开始拼接");
                    ps.setText("");
                    runButton.setEnabled(true); // 恢复按钮
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

        gbc.gridx = 0; gbc.gridy = 6; panel.add(option1, gbc);
        gbc.gridx = 1; gbc.gridy = 6; panel.add(option2, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 3; panel.add(runButton, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3; panel.add(ps, gbc);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 3; panel.add(new JScrollPane(outputArea), gbc);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

    }
}
