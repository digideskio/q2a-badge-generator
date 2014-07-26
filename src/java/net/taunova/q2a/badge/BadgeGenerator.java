package net.taunova.q2a.badge;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * TODO: - improved image quality - user statistics - themes - flexible
 * configuration
 */
public class BadgeGenerator {

    private static final int DEFAULT_TIMEOUT = 5000;
    private static final String SITE_NAME = "site.name";
    private static final String SITE_TITLE = "site.title";
    private static final String COLOR_TEXT = "color.text";
    private static final String COLOR_BAR = "color.bar";

    enum User {

        NAME,
        POINTS,
        RANK,
        QUESTIONS,
        ANSWERS,
        SELECTED,
        IMAGE
    };
    protected Color barColor;
    protected Color textColor;
    protected String siteName;
    protected String cleanName;
    protected String siteText;

    /**
     *
     * @param siteName
     * @param siteText
     * @param textColor
     * @param barColor
     */
    public BadgeGenerator(String siteName, String siteText, Color textColor, Color barColor) {
        this.cleanName = siteName;
        this.siteText = siteText;
        this.textColor = textColor;
        this.siteName = "http://" + siteName;
        this.barColor = barColor;
    }

    protected BufferedImage readBackImage() throws IOException {
        return readImage("background");
    }

    protected BufferedImage readTopImage() throws IOException {
        return readImage("top");
    }

    protected BufferedImage readImage(String name) throws IOException {
        BufferedImage backImage = null;
        String fileName = "resources/" + name + "-" + cleanName + ".png";
        try {
            backImage = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.err.println("Can't read image file: " + fileName);
        }

        return backImage;
    }

    protected BufferedImage createCanvasImage(BufferedImage backImage) {
        ColorModel cm = backImage.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = backImage.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    protected BufferedImage createCanvasImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    protected void process() throws IOException {
        final int TOP_SIZE = 10;

        BufferedImage backImage = readBackImage();
        BufferedImage topImage = readTopImage();

        List<Map<Object, String>> topUsers = new ArrayList<>();

        try {
            URL pageUrl = new URL(siteName + "/users");
            List<String> users = parseUserPage(pageUrl);
            for (String user : users) {
                System.out.println(" --- " + user + " ---------------");
                Map<Object, String> data = parseUser(user);

                if (null != backImage) {
                    generateBadge(user, cleanName, data, createCanvasImage(backImage));
                }

                int rank = Integer.parseInt(data.get(User.RANK));
                if (rank <= TOP_SIZE) {
                    topUsers.add(rank - 1, data);
                }
            }

            if (null != topImage) {
                generateTopImages(topUsers, cleanName, topImage);
            }
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
    }

    public void generateTopImages(List<Map<Object, String>> topUsers, String siteName, BufferedImage topImage) throws IOException {
        int maxScore = getUserScore(topUsers.get(0));

        BufferedImage summaryImage = createCanvasImage(topImage.getWidth(), topUsers.size() * topImage.getHeight());
        Graphics2D summaryGraphics = summaryImage.createGraphics();

        int offset = 0;
        for (Map<Object, String> userData : topUsers) {
            BufferedImage userImage = createCanvasImage(topImage);
            generateTop(siteName, userData, maxScore, userImage);
            summaryGraphics.drawImage(userImage, 0, offset, null);
            offset += topImage.getHeight();
        }

        String resultPath = "result/" + encodeSiteName(siteName);
        checkFilePath(resultPath);
        ImageIO.write(summaryImage, "png", new File(resultPath + File.separator + "000_top.png"));
    }

    public int getUserScore(Map<Object, String> data) {
        String points = data.get(User.POINTS);
        return Integer.parseInt(points.replace(",", ""));

    }

    protected List<String> parseUserPage(URL pageUrl) throws IOException {
        List<String> users = new LinkedList<>();
        Document usersDoc = Util.parse(pageUrl, DEFAULT_TIMEOUT);

        Iterator<Element> userIterator = usersDoc.select("a.qa-user-link").iterator();

        while (userIterator.hasNext()) {
            Element user = userIterator.next();

            String userName = user.text().trim();
            if (!userName.isEmpty()) {
                users.add(userName);
            } else {
                System.out.println("Empty user name found for: " + user.text());
            }
        }
        return users;
    }

    protected String encodeUserName(String userName) {
        return userName.replace(' ', '+');
    }

    protected String encodeSiteName(String siteName) {
        return siteName;
    }

    protected Map<Object, String> parseUser(String userName) throws IOException {
        final String cleanUserName = userName;
        userName = encodeUserName(userName);

        URL userPageUrl = new URL(siteName + "/user/" + userName);
        Document userDoc = Util.parse(userPageUrl, DEFAULT_TIMEOUT);

        Map<Object, String> data = new HashMap<>();

        Object[] values = {
            User.POINTS, "span.qa-uf-user-points", "0",
            User.RANK, "span.qa-uf-user-rank", "-",
            User.QUESTIONS, "span.qa-uf-user-q-posts", "0",
            User.ANSWERS, "span.qa-uf-user-a-posts", "0",
            User.SELECTED, "span.qa-uf-user-a-selecteds", "0",};

        for (int i = 0; i < values.length; i += 3) {
            String value = userDoc.select((String)values[i + 1]).text().trim();
            if (!value.isEmpty()) {
                data.put(values[i], value);
            } else {
                data.put(values[i], values[i + 2].toString());
            }
        }

        String imageUrl = userDoc.select("img.qa-avatar-image").attr("src");

        String imageSrc = (imageUrl.startsWith("http")) ? imageUrl : siteName + "/user/" + imageUrl;
        System.out.println("user image: " + imageSrc);
        data.put(User.IMAGE, imageSrc);
        data.put(User.NAME, cleanUserName);
        return data;
    }

    protected void generateTop(String siteName, Map<Object, String> data, int max, BufferedImage topImage) throws IOException {
        final int rankOffset = 3;
        final int nameOffset = 20;
        final int pointsOffset = 158;

        String userName = data.get(User.NAME);
        Graphics2D backGraphics = (Graphics2D) topImage.getGraphics();

        backGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        backGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        backGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        String rank = data.get(User.RANK);
        String points = data.get(User.POINTS);

        Font f2 = null;
        final int fontSize = 12;
        int size = fontSize;

        final int hoffset = 13;
        final int padding = 4;
        final int limitWidth = 140 - padding;

        while (null == f2) {
            Font f = new Font("Arial", Font.BOLD, size);
            FontMetrics metrics = backGraphics.getFontMetrics(f);
            int textWidth = metrics.stringWidth(userName);
            if (textWidth < limitWidth) {
                f2 = f;
            }
            size--;
        }

        // draw histogram
        final int maxWidth = 135;
        final int wgap = 17;
        final int hgap = 2;

        int score = Integer.parseInt(points.replace(",", ""));
        int wscore = (int) (maxWidth * ((float) score / max));
        backGraphics.setColor(barColor);
        backGraphics.fillRect(wgap, hgap, wscore, topImage.getHeight());

        Font f1 = new Font("Arial", Font.BOLD, fontSize);
        backGraphics.setColor(Color.black);

        backGraphics.setFont(f1);
        backGraphics.drawString(rank, rankOffset, hoffset);
        backGraphics.drawString(points, pointsOffset, hoffset);
        backGraphics.setFont(f2);
        backGraphics.drawString(userName, nameOffset, hoffset);

        String resultPath = "result/" + encodeSiteName(siteName);
        checkFilePath(resultPath);
        ImageIO.write(topImage, "png", new File(resultPath + File.separator + "00" + rank + "_top" + ".png"));
    }

    protected void generateBadge(String userName, String siteName, Map<Object, String> data, BufferedImage backImage) throws IOException {
        int width = backImage.getWidth();
        int height = backImage.getHeight();

        Graphics2D backGraphics = (Graphics2D) backImage.getGraphics();

        backGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        backGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        backGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        String points = data.get(User.POINTS);
        String rank = data.get(User.RANK);
        String answers =  data.get(User.ANSWERS);
        String selected =  data.get(User.SELECTED);
        String questions =  data.get(User.QUESTIONS);

        String line0 = siteName + " - " + siteText;
        String line1 = userName;
        String line2 = points + " баллов (" + rank + " место)";
        String line3 = "Ответов: " + answers + " (" + selected + ")  Вопросов: " + questions;

        Font f0 = new Font("Arial", Font.BOLD, 12);
        Font f2 = new Font("Arial", Font.BOLD, 11);
        Font f3 = new Font("Arial", Font.PLAIN, 11);

        final int xoffset = 4;

        backGraphics.setColor(textColor);
        backGraphics.setFont(f0);
        backGraphics.drawString(line0, xoffset, 11);

        backGraphics.setColor(Color.BLACK);

        Font f1 = null;
        int size = 16;
        final int padding = 4;
        final int limitWidth = width - padding - height;

        while (null == f1) {
            Font f = new Font("Arial", Font.BOLD, size);
            FontMetrics metrics = backGraphics.getFontMetrics(f);
            int textWidth = metrics.stringWidth(line1);
            if (textWidth < limitWidth) {
                f1 = f;
            }
            size--;
        }

        backGraphics.setFont(f1);
        backGraphics.drawString(line1, xoffset, 28);
        backGraphics.setFont(f2);
        backGraphics.drawString(line2, xoffset, 40);
        backGraphics.setFont(f3);
        backGraphics.drawString(line3, xoffset, 52);

        if (data.containsKey(User.IMAGE)) {
            BufferedImage userPic = ImageIO.read(new URL( data.get(User.IMAGE)));

            int yoffset = padding - 1;
            int swidth = height - 2 * padding;

            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8F);
            backGraphics.setComposite(ac);
            backGraphics.drawRect(width - swidth - padding - 1, yoffset - 1, swidth + 2, swidth + 2);
            backGraphics.drawImage(userPic, width - swidth - padding, yoffset, swidth + 1, swidth + 1, null);
        }

        // write image
        String resultPath = "result/" + encodeSiteName(siteName);
        checkFilePath(resultPath);

        ImageIO.write(backImage, "png", new File(resultPath + File.separator + encodeUserName(userName) + ".png"));
    }

    public void checkFilePath(String resultPath) {
        File resultFolder = new File(resultPath);
        if (!resultFolder.isDirectory()) {
            resultFolder.mkdirs();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("you have to specify a property file with all required parameters");
            return;
        }

        Properties p = new Properties();
        p.loadFromXML(new FileInputStream(new File(args[0])));

        BadgeGenerator generator = new BadgeGenerator(p.getProperty(SITE_NAME),
                p.getProperty(SITE_TITLE),
                new Color(Integer.parseInt(p.getProperty(COLOR_TEXT), 16)),
                new Color(Integer.parseInt(p.getProperty(COLOR_BAR), 16)));

        generator.process();
    }
}
