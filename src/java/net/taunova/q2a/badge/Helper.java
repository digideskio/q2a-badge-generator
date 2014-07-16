package net.taunova.q2a.badge;

import java.io.IOException;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * JSoup utilities.
 */
public class Helper {

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
