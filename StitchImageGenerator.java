import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Set;
import java.io.*;

public class StitchImageGenerator {

    public static BufferedImage generateStitchImage(BufferedImage img, List<ColorFile> palette, int n) {
        int stitchWidth = img.getWidth();
        int stitchHeight = img.getHeight();
        int pixelSize = 60; // 刺繍の「目」の解像度
        int outputWidth = stitchWidth * pixelSize;
        int outputHeight = stitchHeight * pixelSize;

        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        // 背景を薄い灰色で塗りつぶし
        g.setColor(new Color(220, 220, 220));
        g.fillRect(0, 0, outputWidth, outputHeight);

        // すべてのピクセルを最も近い色に置き換える
        List<Color> allColors = new ArrayList<>();
        for (int y = 0; y < stitchHeight; y++) {
            for (int x = 0; x < stitchWidth; x++) {
                Color originalColor = new Color(img.getRGB(x, y));
                // 最も近いパレットの色に変換
                Color closestColor = findClosestColor(originalColor, palette);
                allColors.add(closestColor);
            }
        }

        // 上位n色を頻度に基づいて取得(-> topColors)
        Set<Color> topColors = getTopColors(allColors, n);

        // 頻度の高い色の出力
        //System.out.println("Top " + n + " Colors: ");
        //for (Color color : topColors) {
        //    System.out.println(color);
        //}

        // 頻度の高い上位n色を画像として加工して保存
        saveTopColorsAsImage(topColors, "input/colors.csv", "output/top_colors.png");

        // 頻度の高い上位n色の中から最も近い色を選んでピクセルを置き換える
        BufferedImage colorMappedImage = new BufferedImage(stitchWidth, stitchHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < stitchHeight; y++) {
            for (int x = 0; x < stitchWidth; x++) {
                Color originalColor = new Color(img.getRGB(x, y));
                // 最も近い色をtopColorsの中から選択
                Color closestColor = findClosestColorInSet(originalColor, topColors);
                colorMappedImage.setRGB(x, y, closestColor.getRGB());
            }
        }

        // 8近傍ロジックを指定回数繰り返す
        BufferedImage finalImage = new BufferedImage(stitchWidth, stitchHeight, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 10; i++) {
            for (int y = 0; y < stitchHeight; y++) {
                for (int x = 0; x < stitchWidth; x++) {
                    apply8NeighborLogic(finalImage, colorMappedImage, x, y, topColors);
                }
            }
            // finalImageをcolorMappedImageに更新
            colorMappedImage = finalImage;
        }


        // 画像を拡大して描画
        for (int y = 0; y < stitchHeight; y++) {
            for (int x = 0; x < stitchWidth; x++) {
                Color pixelColor = new Color(finalImage.getRGB(x, y));

                // 1x1ピクセルの画像を作成
                BufferedImage colorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                colorImage.setRGB(0, 0, pixelColor.getRGB());

                // リサイズして貼り付け
                int startX = x * pixelSize + 3;
                int startY = y * pixelSize + 3;
                int drawSize = pixelSize - 6;
                BufferedImage resizedImage = ImageResizer.resizeImage(colorImage, drawSize);
                BufferedImage xImage = StitchUtils.leaveOnlyXShape(resizedImage, drawSize);
                g.drawImage(xImage, startX, startY, null);
            }
        }
        g.dispose();
        return output;
    }

    // 刺繍糸の色候補の中から最も近いものを探す
    public static Color findClosestColor(Color target, List<ColorFile> palette) {
        Color closest = null;
        double minDistance = Double.MAX_VALUE;
        for (ColorFile colorFile : palette) {
            double distance = colorDistance(target, new Color(colorFile.r, colorFile.g, colorFile.b));
            if (distance < minDistance) {
                minDistance = distance;
                closest = new Color(colorFile.r, colorFile.g, colorFile.b);
            }
        }
        return closest;
    }

    // どのくらい色が似ているかの数値で評価する
    private static double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

    // 頻度が高い上位n色を取得する
    private static Set<Color> getTopColors(List<Color> colors, int n) {
        Map<Color, Long> colorFrequency = new HashMap<>();
        for (Color color : colors) {
            colorFrequency.put(color, colorFrequency.getOrDefault(color, 0L) + 1);
        }
        // 頻度が高い順に並べて上位n色を選択
        List<Color> sortedColors = colorFrequency.entrySet().stream()
                .sorted((entry1, entry2) -> Long.compare(entry2.getValue(), entry1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Set<Color> topColors = new HashSet<>();
        for (Color color : sortedColors) {
            if (topColors.size() < n) {
                // 新しく選ばれた色とすでに選ばれた色が似ていないか確認
                boolean isSimilar = false;
                for (Color existingColor : topColors) {
                    if (colorDistance(color, existingColor) < 35.0) { // 色が35ピクセル以内に近ければ除外
                        isSimilar = true;
                        break;
                    }
                }
                if (!isSimilar) {
                    topColors.add(color);
                }
            } else {
                break;
            }
        }
        return topColors;
    }

    // 頻度が高い上位n色から最も近いものを選ぶ
    private static Color findClosestColorInSet(Color target, Set<Color> colorSet) {
        Color closest = null;
        double minDistance = Double.MAX_VALUE;
        // topColors内の最も近い色を探す
        for (Color color : colorSet) {
            double distance = colorDistance(target, color);
            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }
        return closest;
    }

    // 8近傍を用いてギザギザ感を低減させる
    private static void apply8NeighborLogic(BufferedImage finalImage, BufferedImage colorMappedImage, int x, int y, Set<Color> topColors) {
        Color currentColor = new Color(colorMappedImage.getRGB(x, y));
        // 8近傍で最も頻度が高い色を取得
        Color mostFrequentNeighborColor = getMostFrequentNeighborColor(colorMappedImage, x, y, topColors);
        // 8近傍の中で現在の色と一致するピクセルが1つ以下の場合に色を置き換える
        int countSameColor = countSameColorInNeighborhood(colorMappedImage, x, y, currentColor);
        if (mostFrequentNeighborColor != null && countSameColor <= 1) {
            // 注目しているピクセルと同じ色が1つ以下ならば、最も頻度の高い色に変更
            finalImage.setRGB(x, y, mostFrequentNeighborColor.getRGB());
        } else {
            // そうでなければ、現在の色を保持
            finalImage.setRGB(x, y, currentColor.getRGB());
        }
    }

    // 8近傍の色を調べてカウントする
    private static int countSameColorInNeighborhood(BufferedImage image, int x, int y, Color targetColor) {
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
        int count = 0;
        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            
            if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                Color neighborColor = new Color(image.getRGB(nx, ny));
                if (neighborColor.equals(targetColor)) {
                    count++;
                }
            }
        }
        return count;
    }

    // 8近傍の色のうち最も頻度の高い色を調べる
    private static Color getMostFrequentNeighborColor(BufferedImage image, int x, int y, Set<Color> topColors) {
        Map<Color, Integer> neighborColorCount = new HashMap<>();
        
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
        
        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            
            if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                Color neighborColor = new Color(image.getRGB(nx, ny));
                if (topColors.contains(neighborColor)) {
                    neighborColorCount.put(neighborColor, neighborColorCount.getOrDefault(neighborColor, 0) + 1);
                }
            }
        }
        return neighborColorCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // csvファイルから色情報を読み込み
    public static List<ColorInfo> loadColorsFromCSV(String csvPath) {
        List<ColorInfo> colors = new ArrayList<>();
        boolean isFirstLine = true;
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] values = line.split(",");
                if (values.length >= 4) {
                    try {
                        String name = values[0].trim(); // 1列目（色名）
                        int r = Integer.parseInt(values[1].trim()); // 2列目（R値）
                        int g = Integer.parseInt(values[2].trim()); // 3列目（G値）
                        int b = Integer.parseInt(values[3].trim()); // 4列目（B値）
                        colors.add(new ColorInfo(name, new Color(r, g, b)));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid RGB values in line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return colors;
    }

    // トップカラーを画像として保存するメソッド
    public static void saveTopColorsAsImage(Set<Color> topColors, String csvPath, String outputPath) {
        List<ColorInfo> colorInfoList = loadColorsFromCSV(csvPath);
        int squareSize = 50; // 各色の正方形のサイズ
        int padding = 10; // 正方形と正方形の間隔
        int imageWidth = squareSize + padding * 2 + 100; // ちょうどいい空白になるように調整
        int imageHeight = (topColors.size() * (squareSize + padding)); // 縦に並べる

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景色を白に設定
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        // トップカラーを描画
        int yPos = padding;
        for (Color color : topColors) {
            // 正方形を描画
            g.setColor(color);
            g.fillRect(padding, yPos, squareSize, squareSize);
            // 対応する文字列を描画
            String colorName = getColorNameFromList(color, colorInfoList);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString(colorName, padding + squareSize + 5, yPos + squareSize / 2);
            yPos += squareSize + padding;
        }
        g.dispose();
        // 画像をファイルに保存
        try {
            File outputFile = new File(outputPath);
            ImageIO.write(image, "PNG", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 色リストから色に対応する名前を取得
    private static String getColorNameFromList(Color color, List<ColorInfo> colorInfoList) {
        for (ColorInfo info : colorInfoList) {
            if (info.color.equals(color)) {
                return info.name;
            }
        }
        return "Unknown"; // 見つからない場合
    }

    // 色と名前のペアを格納するクラス
    public static class ColorInfo {
        String name;
        Color color;
        public ColorInfo(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }
}
