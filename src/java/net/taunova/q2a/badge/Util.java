package net.taunova.q2a.badge;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Various utilities.
 */
public class Util {

    /**
     *
     * @param url
     * @param timeout
     * @return
     * @throws IOException
     */
    public static Document parse(URL url, int timeout) throws IOException {
        Document doc = null;

        final int LIMIT = 10;
        final int LIMIT_SLEEP = 2;

        int iteration = 0;

        while (null == doc) {
            try {
                doc = Jsoup.connect(url.toString())
                        .timeout(timeout)
                        .referrer("http://www.google.com/search")
                        .userAgent("Mozilla/6.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/3.0.0.0")
                        .get();
            } catch (IOException e1) {
                System.out.println("TIMEOUT: refetching: " + iteration);
                if (iteration > LIMIT) {
                    throw e1;
                }

                if (iteration > LIMIT_SLEEP) {
                    sleep(timeout * iteration);
                }

                iteration++;
            }
        }
        return doc;
    }

    /**
     * 
     * @param name
     * @param cleanName
     * @return
     * @throws IOException 
     */
    public static BufferedImage readImage(String name, String cleanName) throws IOException {
        BufferedImage backImage = null;
        String fileName = "resources/" + name + "-" + cleanName + ".png";
        try {
            backImage = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.err.println("Can't read image file: " + fileName);
        }

        return backImage;
    }    
    
    /**
     * 
     * @param backImage
     * @return 
     */
    public static BufferedImage createCanvasImage(BufferedImage backImage) {
        ColorModel cm = backImage.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = backImage.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * 
     * @param width
     * @param height
     * @return 
     */
    public static BufferedImage createCanvasImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }    
    
    /**
     * 
     * @param resultPath 
     */
    public static void checkFilePath(String resultPath) {
        File resultFolder = new File(resultPath);
        if (!resultFolder.isDirectory()) {
            resultFolder.mkdirs();
        }
    }        
    
    /**
     *
     * @param delay
     */
    public static void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (Exception e) {
            //...
        }
    }    
}
