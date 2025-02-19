import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageResizer {
    public static BufferedImage resizeImageWithAspectRatio(BufferedImage img, int targetSize) {
        double imgAspect = (double) img.getWidth() / img.getHeight();

        int newWidth, newHeight;
        if (imgAspect > 1) { // 横長の場合
            newWidth = targetSize;
            newHeight = (int) (targetSize / imgAspect);
        } else { // 縦長または正方形の場合
            newHeight = targetSize;
            newWidth = (int) (targetSize * imgAspect);
        }

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    public static BufferedImage resizeImage(BufferedImage img, int size) {
        BufferedImage resized = new BufferedImage(size, size, img.getType());
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();
        return resized;
    }

    // 画像を指定した高さにリサイズ
    public static BufferedImage resizeToHeight(BufferedImage image, int targetHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 高さを指定して、縦横比を保ちながらリサイズ
        double aspectRatio = (double) width / height;
        int targetWidth = (int) (targetHeight * aspectRatio);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, image.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }
}
