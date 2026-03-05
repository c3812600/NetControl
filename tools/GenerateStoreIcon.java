import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public class GenerateStoreIcon {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: GenerateStoreIcon <srcPng> <outPng>");
        }
        Path src = Path.of(args[0]);
        Path out = Path.of(args[1]);
        BufferedImage input = ImageIO.read(src.toFile());
        if (input == null) throw new IllegalArgumentException("Cannot read image: " + src);
        BufferedImage scaled = scaleToSquare(input, 512);
        ImageIO.write(scaled, "PNG", out.toFile());
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size) {
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        double scale = Math.min((double) size / srcW, (double) size / srcH);
        int drawW = (int) Math.round(srcW * scale);
        int drawH = (int) Math.round(srcH * scale);
        int x = (size - drawW) / 2;
        int y = (size - drawH) / 2;
        g.drawImage(src, x, y, drawW, drawH, null);
        g.dispose();
        return dst;
    }
}
