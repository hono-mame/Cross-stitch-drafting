import java.awt.*;

public class ColorFile {
    int number;
    int r;
    int g;
    int b;

    public ColorFile(int number, int r, int g, int b) {
        this.number = number;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color getColor() {
        return new Color(r, g, b);
    }
}
