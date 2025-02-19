import java.io.*;
import java.util.*;

public class ColorPaletteLoader {
    public static List<ColorFile> loadColorPalette(File colorDir) throws IOException {
        List<ColorFile> colorPalette = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(colorDir));
        String line;

        // ヘッダーは色情報が含まれないのでスキップ
        reader.readLine();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            int number = Integer.parseInt(parts[0].trim());
            int r = Integer.parseInt(parts[1].trim());
            int g = Integer.parseInt(parts[2].trim());
            int b = Integer.parseInt(parts[3].trim());

            ColorFile colorFile = new ColorFile(number, r, g, b);
            colorPalette.add(colorFile);
        }

        reader.close();
        return colorPalette;
    }
}
