import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CrossStitchImage {
    public static void main(String[] args) throws IOException {
        
        /* -------------------- 適宜変更する部分 -------------------- */
        int size = 100;
        int colors = 15;
        String input = "mario.jpeg";
        /* ------------------------------------------------------- */
        input = String.format("input/%s", input);

        // 出力名の調整
        String fileName = new File(input).getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        String output1 = String.format("output/%s_size%d_color%d.jpeg", fileName, size, colors);
        String output2 = String.format("output/%s_size%d_color%d_withinfo.jpeg", fileName, size, colors);

        // 入力画像
        File inputFile = new File(input);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // 色候補CSVファイル
        File colorFile = new File("input/colors.csv");
        List<ColorFile> colorPalette = ColorPaletteLoader.loadColorPalette(colorFile);

        // 入力画像をリサイズ
        BufferedImage resizedImage = ImageResizer.resizeImageWithAspectRatio(inputImage, size);

        // 刺繍図案の生成
        BufferedImage outputImage = StitchImageGenerator.generateStitchImage(resizedImage, colorPalette, colors);

        // 画像を保存
        File outPutFile1 = new File(output1);
        ImageIO.write(outputImage, "png", outPutFile1);
        System.out.println("画像を保存しました: " + outPutFile1.getAbsolutePath());

        // 使用した糸の色情報のpng画像を読み込み
        File topColorsImageFile = new File("output/top_colors.png");
        BufferedImage topColorsImage = ImageIO.read(topColorsImageFile);

        // 画像の縦幅を比較して、縦幅が小さい画像を拡大
        int targetHeight = Math.max(outputImage.getHeight(), topColorsImage.getHeight());

        // 出力画像と糸の色情報の画像を縦幅を合わせてリサイズ
        BufferedImage resizedOutputImage = ImageResizer.resizeToHeight(outputImage, targetHeight);
        BufferedImage resizedTopColorsImage = ImageResizer.resizeToHeight(topColorsImage, targetHeight);

        // 新しい画像の幅と高さを決定
        int totalWidth = resizedOutputImage.getWidth() + resizedTopColorsImage.getWidth();
        int totalHeight = targetHeight;

        // 合成画像を作成
        BufferedImage combinedImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = combinedImage.getGraphics();

        // 出力画像を左に描画
        g.drawImage(resizedOutputImage, 0, 0, null);

        // 糸の色情報の画像を右に描画
        g.drawImage(resizedTopColorsImage, resizedOutputImage.getWidth(), 0, null);
        g.dispose();

        // 背景
        int padding = size;
        int finalWidth = totalWidth + 2 * padding;
        int finalHeight = totalHeight + 2 * padding;
        BufferedImage finalImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = finalImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, finalWidth, finalHeight);

        // 合成画像を中央に描画
        int xOffset = padding;
        int yOffset = padding;
        g2d.drawImage(combinedImage, xOffset, yOffset, null);
        g2d.dispose();

        // 最終的な画像を保存
        File combinedImageFile = new File(output2);
        ImageIO.write(finalImage, "jpeg", combinedImageFile);
        System.out.println("画像を保存しました: " + combinedImageFile.getAbsolutePath());
    }

}
