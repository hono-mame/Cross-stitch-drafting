import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CrossStitchImageGUI {
    private JFrame frame;
    private JTextField sizeField;
    private JTextField colorField;
    private JLabel imageLabel;
    private JLabel fileNameLabel;
    private File selectedImageFile;
    private BufferedImage finalImage; // 保存用

    public CrossStitchImageGUI() {
        frame = new JFrame("Cross Stitch Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // 左側パネル：入力フォーム
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        inputPanel.setBackground(new Color(240, 240, 240));

        JLabel titleLabel = new JLabel("Cross Stitch Generator");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sizeLabel = new JLabel("サイズ (ピクセル):");
        sizeField = new JTextField("150");
        sizeField.setMaximumSize(new Dimension(200, 30));

        JLabel colorLabel = new JLabel("色数:");
        colorField = new JTextField("10");
        colorField.setMaximumSize(new Dimension(200, 30));

        JButton selectImageButton = new JButton("画像を選択");
        selectImageButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectImageButton.addActionListener(e -> chooseImage());

        fileNameLabel = new JLabel("ファイルが選択されていません");
        fileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fileNameLabel.setForeground(Color.GRAY);
        fileNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton executeButton = new JButton("実行");
        executeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        executeButton.addActionListener(new ExecuteButtonListener());

        JButton saveButton = new JButton("保存");
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.addActionListener(new SaveButtonListener());

        // パネル内のコンポーネントの配置
        inputPanel.add(titleLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(sizeLabel);
        inputPanel.add(sizeField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(colorLabel);
        inputPanel.add(colorField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        inputPanel.add(selectImageButton);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(fileNameLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        inputPanel.add(executeButton);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(saveButton);

        inputPanel.setPreferredSize(new Dimension(300, 600));

        // 右側パネル：画像表示スペース
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);
        imageScrollPane.setPreferredSize(new Dimension(500, 500));

        // 全体レイアウト
        frame.add(inputPanel, BorderLayout.WEST);
        frame.add(imageScrollPane, BorderLayout.CENTER);

        frame.setSize(900, 600);
        frame.setVisible(true);
    }

    private void chooseImage() {
        JFileChooser fileChooser = new JFileChooser(new File("input/"));
        fileChooser.setDialogTitle("画像を選択してください");
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            fileNameLabel.setText("選択されたファイル: " + selectedImageFile.getName());
            fileNameLabel.setForeground(Color.BLACK);
        } else {
            JOptionPane.showMessageDialog(frame, "画像が選択されませんでした。");
        }
    }

    private class ExecuteButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedImageFile == null) {
                JOptionPane.showMessageDialog(frame, "まず画像を選択してください。");
                return;
            }

            try {
                int size = Integer.parseInt(sizeField.getText());
                int colors = Integer.parseInt(colorField.getText());

                BufferedImage inputImage = ImageIO.read(selectedImageFile);

                // 色候補CSVファイル
                File colorFile = new File("input/colors.csv");
                List<ColorFile> colorPalette = ColorPaletteLoader.loadColorPalette(colorFile);

                // 入力画像をリサイズ
                BufferedImage resizedImage = ImageResizer.resizeImageWithAspectRatio(inputImage, size);

                // 刺繍図案の生成
                BufferedImage outputImage = StitchImageGenerator.generateStitchImage(resizedImage, colorPalette, colors);

                // 使用した糸の色情報のpng画像を読み込み
                File topColorsImageFile = new File("output/top_colors.png");
                BufferedImage topColorsImage = ImageIO.read(topColorsImageFile);

                // 合成画像の生成
                finalImage = combineImages(outputImage, topColorsImage, size);

                // 画像をリサイズしてGUIに表示
                Image scaledImage = finalImage.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                frame.repaint();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "エラーが発生しました: " + ex.getMessage());
            }
        }

        private BufferedImage combineImages(BufferedImage outputImage, BufferedImage topColorsImage, int size) {
            int targetHeight = Math.max(outputImage.getHeight(), topColorsImage.getHeight());
            BufferedImage resizedOutputImage = ImageResizer.resizeToHeight(outputImage, targetHeight);
            BufferedImage resizedTopColorsImage = ImageResizer.resizeToHeight(topColorsImage, targetHeight);

            int totalWidth = resizedOutputImage.getWidth() + resizedTopColorsImage.getWidth();
            int totalHeight = targetHeight;
            BufferedImage combinedImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics g = combinedImage.getGraphics();
            g.drawImage(resizedOutputImage, 0, 0, null);
            g.drawImage(resizedTopColorsImage, resizedOutputImage.getWidth(), 0, null);
            g.dispose();

            int padding = size;
            int finalWidth = totalWidth + 2 * padding;
            int finalHeight = totalHeight + 2 * padding;
            BufferedImage finalImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = finalImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, finalWidth, finalHeight);
            g2d.drawImage(combinedImage, padding, padding, null);
            g2d.dispose();

            return finalImage;
        }
    }

    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (finalImage == null) {
                JOptionPane.showMessageDialog(frame, "保存する画像がありません。");
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存先を選択してください");
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                try {
                    ImageIO.write(finalImage, "png", saveFile);
                    JOptionPane.showMessageDialog(frame, "画像を保存しました: " + saveFile.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "画像の保存に失敗しました: " + ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossStitchImageGUI::new);
    }
}
