package qz.printer.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class WebAppTest {

    private static final Logger log = LoggerFactory.getLogger(WebAppTest.class);

    public static void main(String[] args) throws Throwable {
        WebApp.initialize();

        boolean audit = false;
        if (args.length > 0) { audit = Boolean.parseBoolean(args[0]) || "1".equals(args[0]);}
        int knownHeightTests = 1000;
        if (args.length > 1) { knownHeightTests = Integer.parseInt(args[1]);}
        int fitToHeightTests = 1000;
        if (args.length > 2) { fitToHeightTests = Integer.parseInt(args[2]);}

        if (!testKnownSize(knownHeightTests, audit)) {
            log.error("Testing well defined sizes failed");
        } else if (!testFittedSize(fitToHeightTests, audit)) {
            log.error("Testing fit to height sizing failed");
        } else {
            log.info("All tests passed");
        }

        System.exit(0); //explicit exit since jfx is running in background
    }


    public static boolean testKnownSize(int trials, boolean enableAuditing) throws Throwable {
        for(int i = 0; i < trials; i++) {
            //new size every run
            double printW = Math.max(2, (int)(Math.random() * 110) / 10d) * 72d;
            double printH = Math.max(3, (int)(Math.random() * 110) / 10d) * 72d;
            double zoom = Math.max(0.5d, (int)(Math.random() * 30) / 10d);

            BufferedImage sample = attemptCapture("known-" + i, printW, printH, zoom, enableAuditing && Math.random() < 0.1);

            if (sample == null) {
                log.error("Failed to create capture");
                return false;
            }

            //check capture for dimensional accuracy within 1 pixel of expected (due to int rounding)
            int expectedWidth = (int)Math.round(printW * (96d / 72d) * zoom);
            int expectedHeight = (int)Math.round(printH * (96d / 72d) * zoom);
            boolean result = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                result = false;
            }
            if (!Arrays.asList(expectedHeight, expectedHeight + 1, expectedHeight - 1).contains(sample.getHeight())) {
                log.error("Expected height to be {} but got {}", expectedHeight, sample.getHeight());
                result = false;
            }

            if (!result) {
                if (enableAuditing) {
                    saveAudit("invalid-", sample);
                }

                return false;
            }
        }

        return true;
    }

    public static boolean testFittedSize(int trials, boolean enableAuditing) throws Throwable {
        for(int i = 0; i < trials; i++) {
            //new size every run (height always starts at 0)
            double printW = Math.max(2, (int)(Math.random() * 110) / 10d) * 72d;
            double zoom = Math.max(0.5d, (int)(Math.random() * 30) / 10d);

            BufferedImage sample = attemptCapture("fitted-" + i, printW, 0, zoom, enableAuditing && Math.random() < 0.1);

            if (sample == null) {
                log.error("Failed to create capture");
                return false;
            }

            //check capture for dimensional accuracy within 1 pixel of expected (due to int rounding)
            //expected height is not known for these tests
            int expectedWidth = (int)Math.round(printW * (96d / 72d) * zoom);
            boolean result = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                result = false;
            }

            if (!result) {
                if (enableAuditing) {
                    saveAudit("invalid-", sample);
                }

                return false;
            }
        }

        return true;
    }

    private static BufferedImage attemptCapture(String index, double width, double height, double zoom, boolean audit) throws Throwable {
        int hue = (int)(Math.random() * 360);
        int level = (int)(Math.random() * 50) + 25;

        WebAppModel model = new WebAppModel("<html>" +
                                                    "<body style='background-color: hsl(" + hue + ","+ level +"%,"+ level +"%);'>" +
                                                    "   <table style='font-family: monospace; border: 1px;'>" +
                                                    "       <tr style='height: 6cm;'>" +
                                                    "           <td valign='top'>Generated content:</td>" +
                                                    "           <td valign='top'><b>" + index + "</b></td>" +
                                                    "       </tr>" +
                                                    "       <tr>" +
                                                    "           <td>Content size:</td>" +
                                                    "           <td>" + width + "x" + height + "</td>" +
                                                    "       </tr>" +
                                                    "       <tr>" +
                                                    "           <td>Zoomed to</td>" +
                                                    "           <td>x " + zoom + "</td>" +
                                                    "       </tr>" +
                                                    "   </table>" +
                                                    "</body>" +
                                                    "</html>",
                                            true, width, height, true, zoom);

        log.trace("Generating #{} = [({},{}), x{}]", index, model.getWebWidth(), model.getWebHeight(), model.getZoom());
        BufferedImage capture = WebApp.capture(model);

        //TODO - check bottom right matches expected color

        if (audit) {
            saveAudit(index, capture);
        }

        return capture;
    }

    private static void saveAudit(String id, BufferedImage capture) throws IOException {
        File temp = File.createTempFile("qz-" + id, ".png");
        ImageIO.write(capture, "png", temp);

        log.info("Sampled {}: {}", id, temp.getName());
    }

}
