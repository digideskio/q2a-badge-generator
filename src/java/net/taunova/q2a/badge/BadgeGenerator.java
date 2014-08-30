package net.taunova.q2a.badge;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * TODO: - improved image quality - user statistics - themes - flexible
 * configuration
 */
public class BadgeGenerator {
    private static final int TOP_SIZE = 10;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final String SITE_NAME = "site.name";
    private static final String SITE_TITLE = "site.title";
    private static final String COLOR_TEXT = "color.text";
    private static final String COLOR_BAR = "color.bar";

    enum User {
        NAME,
        POINTS,
        SCORE,
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
        return Util.readImage("background", cleanName);
    }

    protected BufferedImage readTopImage() throws IOException {
        return Util.readImage("top", cleanName);
    }

    protected void process() throws IOException {
        BufferedImage backImage = readBackImage();
        BufferedImage topImage = readTopImage();

        Map<String, UserData> rankMap = new TreeMap<>();
        
        try {
            URL pageUrl = new URL(siteName + "/users");
            List<String> users = parseUserPage(pageUrl);
            for (String user : users) {
                System.out.println(" --- " + user + " ---------------");
                UserData data = parseUser(user);
                System.out.println("    " + data);
                
                if (null != backImage) {
                    generateBadge(user, cleanName, data, Util.createCanvasImage(backImage));
                }
              
                // Some users can have same score, so key should be unique
                int score = Integer.parseInt(data.getAttr(User.SCORE));
                String key = String.format("%010d", score) + "-" + user;
                rankMap.put(key, data);
            }

            if (null != topImage) {
                generateTopImages(rankMap, cleanName, topImage);
            }
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
    }
   
    public void generateTopImages(Map<String, UserData> rankMap, String siteName, BufferedImage topImage) throws IOException {
        Iterator<UserData> users = rankMap.values().iterator();        
        int maxScore = users.next().getUserScore();
        
        users = rankMap.values().iterator();

        BufferedImage summaryImage = Util.createCanvasImage(topImage.getWidth(), TOP_SIZE * topImage.getHeight());
        Graphics2D summaryGraphics = summaryImage.createGraphics();

        int offset = 0;
        int iteration = TOP_SIZE;
                
        while (users.hasNext() && iteration-- > 0) {
            UserData userData = users.next();
            System.out.println(" user rank: " + userData.getAttr(User.SCORE));
            BufferedImage userImage = Util.createCanvasImage(topImage);
            generateTop(siteName, userData, maxScore, userImage);
            summaryGraphics.drawImage(userImage, 0, offset, null);
            offset += topImage.getHeight();            
        }

        String resultPath = "result/" + encodeSiteName(siteName);
        Util.checkFilePath(resultPath);
        ImageIO.write(summaryImage, "png", new File(resultPath + File.separator + "000_top.png"));
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

    protected UserData parseUser(String userName) throws IOException {
        final String cleanUserName = userName;
        userName = encodeUserName(userName);

        URL userPageUrl = new URL(siteName + "/user/" + userName);
        Document userDoc = Util.parse(userPageUrl, DEFAULT_TIMEOUT);

        UserData data = new UserData(userName);

        Object[] values = {
            User.POINTS, "span.qa-uf-user-points", "0",
            User.SCORE, "span.qa-uf-user-rank", "-",
            User.QUESTIONS, "span.qa-uf-user-q-posts", "0",
            User.ANSWERS, "span.qa-uf-user-a-posts", "0",
            User.SELECTED, "span.qa-uf-user-a-selecteds", "0",};

        for (int i = 0; i < values.length; i += 3) {
            String value = userDoc.select((String) values[i + 1]).text().trim();
            if (!value.isEmpty()) {
                data.putAttr(values[i], value);
            } else {
                data.putAttr(values[i], values[i + 2].toString());
            }
        }

        String imageUrl = userDoc.select("img.qa-avatar-image").attr("src");

        String imageSrc = (imageUrl.startsWith("http")) ? imageUrl : siteName + "/user/" + imageUrl;
        System.out.println("user image: " + imageSrc);
        data.putAttr(User.IMAGE, imageSrc);
        data.putAttr(User.NAME, cleanUserName);
        return data;
    }

    protected void generateTop(String siteName, UserData data, int max, BufferedImage topImage) throws IOException {
        final int rankOffset = 3;
        final int nameOffset = 20;
        final int pointsOffset = 158;

        String userName = data.getAttr(User.NAME);
        Graphics2D backGraphics = (Graphics2D) topImage.getGraphics();

        backGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        backGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        backGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        String rank = data.getAttr(User.SCORE);
        String points = data.getAttr(User.POINTS);

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
        Util.checkFilePath(resultPath);
        ImageIO.write(topImage, "png", new File(resultPath + File.separator + "00" + rank + "_top" + ".png"));
    }

    protected void generateBadge(String userName, String siteName, UserData data, BufferedImage backImage) throws IOException {
        int width = backImage.getWidth();
        int height = backImage.getHeight();

        Graphics2D backGraphics = (Graphics2D) backImage.getGraphics();

        backGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        backGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        backGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        String points = data.getAttr(User.POINTS);
        String rank = data.getAttr(User.SCORE);
        String answers = data.getAttr(User.ANSWERS);
        String selected = data.getAttr(User.SELECTED);
        String questions = data.getAttr(User.QUESTIONS);

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

        if (data.containsAttr(User.IMAGE)) {
            BufferedImage userPic = ImageIO.read(new URL(data.getAttr(User.IMAGE)));

            int yoffset = padding - 1;
            int swidth = height - 2 * padding;

            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8F);
            backGraphics.setComposite(ac);
            backGraphics.drawRect(width - swidth - padding - 1, yoffset - 1, swidth + 2, swidth + 2);
            backGraphics.drawImage(userPic, width - swidth - padding, yoffset, swidth + 1, swidth + 1, null);
        }

        // write image
        String resultPath = "result/" + encodeSiteName(siteName);
        Util.checkFilePath(resultPath);

        ImageIO.write(backImage, "png", new File(resultPath + File.separator + encodeUserName(userName) + ".png"));
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
