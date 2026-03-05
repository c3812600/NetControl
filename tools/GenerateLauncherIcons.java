import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class GenerateLauncherIcons {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: GenerateLauncherIcons <srcPng> <resDir>");
        }
        Path src = Path.of(args[0]);
        Path resDir = Path.of(args[1]);

        BufferedImage input = ImageIO.read(src.toFile());
        if (input == null) {
            throw new IllegalArgumentException("Cannot read image: " + src);
        }

        Map<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("mipmap-mdpi", 48);
        sizes.put("mipmap-hdpi", 72);
        sizes.put("mipmap-xhdpi", 96);
        sizes.put("mipmap-xxhdpi", 144);
        sizes.put("mipmap-xxxhdpi", 192);

        for (Map.Entry<String, Integer> e : sizes.entrySet()) {
            String dirName = e.getKey();
            int size = e.getValue();
            Path outDir = resDir.resolve(dirName);
            Files.createDirectories(outDir);

            // 缩放图片（保持原图边距，不再自动添加边距）
            // 1. 生成普通图标 (ic_launcher.png)
            BufferedImage scaled = scaleToSquare(input, size);
            File outputFile = outDir.resolve("ic_launcher.png").toFile();
            ImageIO.write(scaled, "png", outputFile);
            System.out.println("Generated: " + outputFile.getAbsolutePath());

            // 2. 生成圆形图标 (ic_launcher_round.png) - 同尺寸
            File roundFile = outDir.resolve("ic_launcher_round.png").toFile();
            ImageIO.write(scaled, "png", roundFile);
            System.out.println("Generated: " + roundFile.getAbsolutePath());

            // 3. 生成自适应图标前景 (ic_launcher_foreground.png) - 尺寸为 108/48 倍 (2.25倍)
            int foregroundSize = (int) (size * 2.25);
            BufferedImage foreground = scaleToSquare(input, foregroundSize);
            File foregroundFile = outDir.resolve("ic_launcher_foreground.png").toFile();
            ImageIO.write(foreground, "png", foregroundFile);
            System.out.println("Generated: " + foregroundFile.getAbsolutePath());
        }
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size) {
        return scaleToSquare(src, size, 0.0);
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size, double marginRatio) {
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int contentSize = (int) Math.round(size * Math.max(0.0, 1.0 - 2.0 * marginRatio));
        double scale = Math.min((double) contentSize / srcW, (double) contentSize / srcH);
        int drawW = (int) Math.round(srcW * scale);
        int drawH = (int) Math.round(srcH * scale);
        int x = (size - drawW) / 2;
        int y = (size - drawH) / 2;
        g.drawImage(src, x, y, drawW, drawH, null);
        g.dispose();
        return dst;
    }

    private static void writePng(Path out, BufferedImage image) throws Exception {
        File f = out.toFile();
        Files.createDirectories(out.getParent());
        if (!ImageIO.write(image, "PNG", f)) {
            throw new IllegalStateException("No PNG writer available");
        }
    }
}
