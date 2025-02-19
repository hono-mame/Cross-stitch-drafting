import java.awt.*;
import java.awt.image.BufferedImage;

public class StitchUtils {
    public static BufferedImage leaveOnlyXShape(BufferedImage image, int size) {
        BufferedImage xImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = xImage.createGraphics();

        // 背景を透明に設定
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);

        // 描画モードを戻す
        g.setComposite(AlphaComposite.SrcOver);

        // 元画像を描画
        int imageSize = Math.min(image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, size, size, 0, 0, imageSize, imageSize, null);

        // ステッチの斜めライン部分以外を透明化
        g.setComposite(AlphaComposite.Clear); // 周囲を透明化
        int lineWidth = size / 3; // バツ印の線幅

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean onLine1 = Math.abs(x - y) < lineWidth; // 左上→右下
                boolean onLine2 = Math.abs(x + y - size + 1) < lineWidth; // 左下→右上
                // バツ印の斜めライン以外を透明にする
                if (!onLine1 && !onLine2) {
                    xImage.setRGB(x, y, 0); // 完全に透明化
                }
            }
        }
        g.dispose();
        return xImage;
    }
}
