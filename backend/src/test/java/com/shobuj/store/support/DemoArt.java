package com.shobuj.store.support;

import java.util.Locale;
import java.util.Random;

/**
 * Draws potted plants as SVG, standing in for product photos in demo data. Canvas is 600 x 750
 * (4:5, the product image ratio). Drawings are seeded, so the same plant always looks the same.
 */
public final class DemoArt {

    public enum Form { FRUIT_TREE, FLOWER_TREE, SAPLING, PALM, SNAKE, MONSTERA, TRAILING, BONSAI, COLUMN }

    public enum Pot { GROW_BAG, CLAY, CERAMIC }

    /** How a plant looks: its shape, leaf colour and (optional) fruit or flower colour. */
    public record Plant(Form form, String leaf, String accent, long seed) {
    }

    private DemoArt() {
    }

    public static String product(Plant plant, Pot pot, String potHex) {
        return svg("0 0 600 750", backdrop(plant, 600, 750) + scene(plant, pot, potHex, "p", ""));
    }

    /** A close-up of the leaves, the second image on cards and product pages. */
    public static String detail(Plant plant, Pot pot, String potHex) {
        String crop = switch (plant.form()) {
            case SNAKE, TRAILING, MONSTERA -> "130 180 340 425";
            case BONSAI -> "120 230 360 450";
            default -> "140 90 320 400";
        };
        return svg(crop, backdrop(plant, 600, 750) + scene(plant, pot, potHex, "p", ""));
    }

    /** A wide banner with several plants, for collections and the home page. */
    public static String banner(String background, Object[]... plants) {
        StringBuilder body = new StringBuilder();
        body.append("<rect width=\"1200\" height=\"800\" fill=\"").append(background).append("\"/>")
                .append("<rect width=\"1200\" height=\"800\" fill=\"url(#bl)\"/>")
                .append("<rect y=\"640\" width=\"1200\" height=\"160\" fill=\"#000\" opacity=\".05\"/>");
        int n = plants.length;
        double[] xs = n >= 3 ? new double[] {230, 600, 970} : new double[] {400, 800};
        double[] scales = n >= 3 ? new double[] {0.86, 1.0, 0.8} : new double[] {0.98, 0.9};
        for (int i = 0; i < n && i < xs.length; i++) {
            Object[] item = plants[i];
            double s = scales[i];
            String transform = String.format(Locale.ROOT, "translate(%.0f %.0f) scale(%.2f)",
                    xs[i] - 300 * s, 740 - 700 * s, s);
            body.append(scene((Plant) item[0], (Pot) item[1], (String) item[2], "b" + i, transform));
        }
        String defs = "<defs><radialGradient id=\"bl\" cx=\"50%\" cy=\"25%\" r=\"85%\">"
                + "<stop offset=\"0\" stop-color=\"#fff\" stop-opacity=\".28\"/>"
                + "<stop offset=\"1\" stop-color=\"#000\" stop-opacity=\".10\"/></radialGradient></defs>";
        return svg("0 0 1200 800", defs + body);
    }

    private static String svg(String viewBox, String content) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"" + viewBox
                + "\" preserveAspectRatio=\"xMidYMid slice\">" + content + "</svg>";
    }

    private static String backdrop(Plant plant, int w, int h) {
        String base = mix("#ECEAE1", plant.leaf(), 0.06);
        return "<defs><radialGradient id=\"bg\" cx=\"50%\" cy=\"35%\" r=\"80%\">"
                + "<stop offset=\"0\" stop-color=\"" + lighten(base, .35) + "\"/>"
                + "<stop offset=\"1\" stop-color=\"" + darken(base, .07) + "\"/></radialGradient></defs>"
                + "<rect width=\"" + w + "\" height=\"" + h + "\" fill=\"url(#bg)\"/>"
                + "<rect y=\"610\" width=\"" + w + "\" height=\"" + (h - 610) + "\" fill=\"#000\" opacity=\".035\"/>";
    }

    // ------------------------------------------------------------------ scene

    private static String scene(Plant plant, Pot pot, String potHex, String id, String transform) {
        Random r = new Random(plant.seed());
        StringBuilder s = new StringBuilder("<g" + (transform.isEmpty() ? "" : " transform=\"" + transform + "\"") + ">");
        s.append("<defs><filter id=\"").append(id).append("sh\" x=\"-30%\" y=\"-30%\" width=\"160%\" height=\"160%\">")
                .append("<feGaussianBlur stdDeviation=\"14\"/></filter>")
                .append("<filter id=\"").append(id).append("ds\" x=\"-20%\" y=\"-20%\" width=\"140%\" height=\"140%\">")
                .append("<feDropShadow dx=\"0\" dy=\"8\" stdDeviation=\"10\" flood-color=\"#1d2a1f\" flood-opacity=\".18\"/></filter>")
                .append("</defs>");
        boolean bonsai = plant.form() == Form.BONSAI;
        // Floor shadow.
        s.append("<ellipse cx=\"300\" cy=\"").append(bonsai ? 668 : 692).append("\" rx=\"").append(bonsai ? 190 : 150)
                .append("\" ry=\"18\" fill=\"#1d2a1f\" opacity=\".22\" filter=\"url(#").append(id).append("sh)\"/>");
        String plantSvg = switch (plant.form()) {
            case FRUIT_TREE -> tree(plant, r, true, false);
            case FLOWER_TREE -> tree(plant, r, false, true);
            case SAPLING -> sapling(plant, r);
            case PALM -> palm(plant, r);
            case SNAKE -> snake(plant, r);
            case MONSTERA -> monstera(plant, r);
            case TRAILING -> trailing(plant, r, false);
            case BONSAI -> bonsai(plant, r);
            case COLUMN -> column(plant, r);
        };
        s.append("<g filter=\"url(#").append(id).append("ds)\">").append(plantSvg).append("</g>");
        s.append(bonsai ? tray(potHex) : pot(pot, potHex));
        if (plant.form() == Form.TRAILING) {
            s.append(trailing(plant, new Random(plant.seed() + 7), true));
        }
        return s.append("</g>").toString();
    }

    // ------------------------------------------------------------------ pots

    private static String pot(Pot pot, String hex) {
        String dark = darken(hex, .25);
        String light = lighten(hex, .18);
        String soil = "<ellipse cx=\"300\" cy=\"556\" rx=\"96\" ry=\"14\" fill=\"#3E2C20\"/>"
                + "<ellipse cx=\"300\" cy=\"553\" rx=\"86\" ry=\"9\" fill=\"#5A4030\"/>";
        return switch (pot) {
            case GROW_BAG -> soil
                    + "<path d=\"M196 556 Q188 620 204 690 Q300 708 396 690 Q412 620 404 556 Q300 572 196 556 Z\" fill=\"" + hex + "\"/>"
                    + "<path d=\"M196 556 Q300 574 404 556 L402 574 Q300 592 198 574 Z\" fill=\"" + light + "\" opacity=\".55\"/>"
                    + "<path d=\"M228 600 Q240 640 232 684 M300 610 Q306 650 298 698 M366 598 Q356 640 368 684\" stroke=\"#000\" stroke-width=\"3\" opacity=\".18\" fill=\"none\"/>"
                    + "<path d=\"M214 586 Q230 640 220 680\" stroke=\"#fff\" stroke-width=\"5\" opacity=\".08\" fill=\"none\"/>";
            case CLAY -> soil
                    + "<path d=\"M210 580 L390 580 L368 690 Q300 700 232 690 Z\" fill=\"" + hex + "\"/>"
                    + "<path d=\"M210 580 L246 580 L262 694 Q244 693 232 690 Z\" fill=\"#fff\" opacity=\".10\"/>"
                    + "<path d=\"M350 580 L390 580 L368 690 Q356 692 344 693 Z\" fill=\"#000\" opacity=\".12\"/>"
                    + "<rect x=\"192\" y=\"548\" width=\"216\" height=\"38\" rx=\"6\" fill=\"" + light + "\"/>"
                    + "<rect x=\"192\" y=\"578\" width=\"216\" height=\"8\" fill=\"" + dark + "\" opacity=\".45\"/>";
            case CERAMIC -> soil
                    + "<path d=\"M198 548 L402 548 L396 650 Q394 694 352 694 L248 694 Q206 694 204 650 Z\" fill=\"" + hex + "\"/>"
                    + "<path d=\"M218 560 Q214 620 226 680\" stroke=\"#fff\" stroke-width=\"12\" stroke-linecap=\"round\" opacity=\".22\" fill=\"none\"/>"
                    + "<path d=\"M380 556 Q386 620 372 684\" stroke=\"#000\" stroke-width=\"16\" stroke-linecap=\"round\" opacity=\".08\" fill=\"none\"/>"
                    + "<rect x=\"198\" y=\"546\" width=\"204\" height=\"8\" rx=\"4\" fill=\"" + dark + "\" opacity=\".35\"/>";
        };
    }

    private static String tray(String hex) {
        return "<ellipse cx=\"300\" cy=\"612\" rx=\"170\" ry=\"14\" fill=\"#3E2C20\"/>"
                + "<path d=\"M126 612 L474 612 L452 660 L148 660 Z\" fill=\"" + hex + "\"/>"
                + "<rect x=\"118\" y=\"604\" width=\"364\" height=\"14\" rx=\"5\" fill=\"" + lighten(hex, .15) + "\"/>"
                + "<rect x=\"170\" y=\"660\" width=\"24\" height=\"10\" fill=\"" + darken(hex, .3) + "\"/>"
                + "<rect x=\"406\" y=\"660\" width=\"24\" height=\"10\" fill=\"" + darken(hex, .3) + "\"/>";
    }

    // ------------------------------------------------------------------ plants

    private static String trunk(String color, double x0, double y0, double x1, double y1, double w0, double w1) {
        double mx = (x0 + x1) / 2 + (x1 - x0) * 0.2;
        return "<path d=\"M" + f(x0 - w0) + " " + f(y0) + " Q" + f(mx - (w0 + w1) / 2) + " " + f((y0 + y1) / 2) + " "
                + f(x1 - w1) + " " + f(y1) + " L" + f(x1 + w1) + " " + f(y1) + " Q" + f(mx + (w0 + w1) / 2) + " "
                + f((y0 + y1) / 2) + " " + f(x0 + w0) + " " + f(y0) + " Z\" fill=\"" + color + "\"/>";
    }

    private static String tree(Plant p, Random r, boolean fruit, boolean flowers) {
        String bark = "#6B4A33";
        StringBuilder s = new StringBuilder();
        s.append(trunk(bark, 300, 560, 296, 300, 13, 7));
        s.append(trunk(bark, 298, 420, 236, 300, 6, 3)).append(trunk(bark, 298, 400, 366, 290, 6, 3));
        // Canopy: overlapping blobs in three shades, darkest at the back.
        String[] shades = {darken(p.leaf(), .22), p.leaf(), lighten(p.leaf(), .16)};
        double[][] blobs = {{300, 250, 120}, {214, 285, 82}, {388, 282, 86}, {250, 196, 82}, {352, 192, 84},
            {300, 152, 70}, {190, 340, 54}, {410, 336, 56}, {300, 320, 90}};
        for (int layer = 0; layer < 3; layer++) {
            for (double[] b : blobs) {
                double jx = (r.nextDouble() - .5) * 26;
                double jy = (r.nextDouble() - .5) * 22 - layer * 8;
                double rad = b[2] * (1 - layer * 0.22);
                s.append(circle(b[0] + jx, b[1] + jy, rad, shades[layer]));
            }
        }
        s.append(leafSpeckles(r, 300, 250, 170, 140, lighten(p.leaf(), .3), 60));
        if (fruit && p.accent() != null) {
            for (int i = 0; i < 16; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(r.nextDouble());
                double x = 300 + Math.cos(a) * d * 150;
                double y = 255 + Math.sin(a) * d * 115;
                s.append("<ellipse cx=\"").append(f(x)).append("\" cy=\"").append(f(y)).append("\" rx=\"11\" ry=\"14\" fill=\"")
                        .append(p.accent()).append("\"/>")
                        .append(circle(x - 3.5, y - 5, 3.5, "#fff\" opacity=\".45"));
            }
        }
        if (flowers && p.accent() != null) {
            for (int i = 0; i < 70; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(r.nextDouble());
                double x = 300 + Math.cos(a) * d * 165;
                double y = 245 + Math.sin(a) * d * 125;
                s.append(circle(x, y, 5 + r.nextDouble() * 7, i % 3 == 0 ? lighten(p.accent(), .2) : p.accent()));
            }
        }
        return s.toString();
    }

    private static String sapling(Plant p, Random r) {
        StringBuilder s = new StringBuilder(trunk("#7A5A3E", 300, 560, 302, 110, 7, 3));
        String[] shades = {darken(p.leaf(), .15), p.leaf(), lighten(p.leaf(), .15)};
        for (int i = 0; i < 9; i++) {
            double y = 150 + i * 42;
            int side = i % 2 == 0 ? -1 : 1;
            double len = 70 + (8 - i) * 6 + r.nextDouble() * 20;
            double ex = 300 + side * len;
            double ey = y - 30;
            s.append("<path d=\"M301 ").append(f(y)).append(" Q").append(f(300 + side * len * .5)).append(" ").append(f(y - 8))
                    .append(" ").append(f(ex)).append(" ").append(f(ey)).append("\" stroke=\"#6E5238\" stroke-width=\"3\" fill=\"none\"/>");
            for (int k = 1; k <= 6; k++) {
                double t = k / 6.0;
                double lx = 300 + side * len * t;
                double ly = y - 8 * t - 22 * t * t;
                for (int up : new int[] {-1, 1}) {
                    double ang = Math.toDegrees(Math.atan2(-30, side * len)) + up * 50;
                    s.append(leaf(lx, ly, 22, 8, ang, shades[(k + i) % 3]));
                }
            }
        }
        for (int k = 0; k < 5; k++) {
            s.append(leaf(302 + (r.nextDouble() - .5) * 12, 112 + k * 8, 24, 8, -90 + (k - 2) * 30, shades[k % 3]));
        }
        return s.toString();
    }

    private static String palm(Plant p, Random r) {
        StringBuilder s = new StringBuilder();
        String[] shades = {darken(p.leaf(), .18), p.leaf(), lighten(p.leaf(), .14)};
        int stems = 3;
        for (int st = 0; st < stems; st++) {
            double sx = 300 + (st - 1) * 22;
            double tx = 300 + (st - 1) * 70;
            double ty = 230 - (st == 1 ? 60 : 0);
            s.append("<path d=\"M").append(f(sx)).append(" 556 Q").append(f((sx + tx) / 2)).append(" 400 ")
                    .append(f(tx)).append(" ").append(f(ty)).append("\" stroke=\"").append(darken(p.leaf(), .3))
                    .append("\" stroke-width=\"7\" fill=\"none\" stroke-linecap=\"round\"/>");
            for (int fr = 0; fr < 6; fr++) {
                double ang = Math.toRadians(-160 + fr * 28 + (r.nextDouble() - .5) * 10);
                double len = 150 + r.nextDouble() * 50;
                double ex = tx + Math.cos(ang) * len;
                double ey = ty + Math.sin(ang) * len * .7 + len * .35;
                double cx = tx + Math.cos(ang) * len * .5;
                double cy = ty + Math.sin(ang) * len * .55 - 20;
                String shade = shades[(fr + st) % 3];
                s.append("<path d=\"M").append(f(tx)).append(" ").append(f(ty)).append(" Q").append(f(cx)).append(" ")
                        .append(f(cy)).append(" ").append(f(ex)).append(" ").append(f(ey)).append("\" stroke=\"")
                        .append(darken(shade, .2)).append("\" stroke-width=\"3\" fill=\"none\"/>");
                for (int k = 2; k <= 14; k++) {
                    double t = k / 15.0;
                    double x = (1 - t) * (1 - t) * tx + 2 * (1 - t) * t * cx + t * t * ex;
                    double y = (1 - t) * (1 - t) * ty + 2 * (1 - t) * t * cy + t * t * ey;
                    double dir = Math.toDegrees(Math.atan2(ey - ty, ex - tx));
                    double l = 34 * (1 - t * .6);
                    s.append(leaf(x, y, l, 4.5, dir + 55, shade)).append(leaf(x, y, l, 4.5, dir - 55, shade));
                }
            }
        }
        return s.toString();
    }

    private static String snake(Plant p, Random r) {
        StringBuilder s = new StringBuilder();
        String edge = p.accent() == null ? lighten(p.leaf(), .3) : p.accent();
        double[] xs = {240, 268, 296, 322, 350, 372, 284, 312};
        for (int i = 0; i < xs.length; i++) {
            double x = xs[i];
            double top = 150 + r.nextDouble() * 120 + (i >= 6 ? 60 : 0);
            double lean = (x - 300) * .6 + (r.nextDouble() - .5) * 20;
            double w = 22 + r.nextDouble() * 8;
            String shade = i % 2 == 0 ? p.leaf() : darken(p.leaf(), .12);
            String d = "M" + f(x - w) + " 556 Q" + f(x - w * .9 + lean * .3) + " " + f((556 + top) / 2) + " "
                    + f(x + lean) + " " + f(top) + " Q" + f(x + w * .9 + lean * .3) + " " + f((556 + top) / 2) + " "
                    + f(x + w) + " 556 Z";
            s.append("<path d=\"").append(d).append("\" fill=\"").append(edge).append("\"/>");
            String inner = "M" + f(x - w + 5) + " 556 Q" + f(x - w * .9 + lean * .3 + 5) + " " + f((556 + top) / 2) + " "
                    + f(x + lean) + " " + f(top + 14) + " Q" + f(x + w * .9 + lean * .3 - 5) + " " + f((556 + top) / 2) + " "
                    + f(x + w - 5) + " 556 Z";
            s.append("<path d=\"").append(inner).append("\" fill=\"").append(shade).append("\"/>");
            for (int b = 0; b < 7; b++) {
                double y = 530 - b * (556 - top) / 8.0;
                double bx = x + lean * (556 - y) / (556 - top) * .8;
                s.append("<path d=\"M").append(f(bx - w * .55)).append(" ").append(f(y)).append(" q").append(f(w * .55))
                        .append(" -10 ").append(f(w * 1.1)).append(" 0\" stroke=\"").append(lighten(shade, .25))
                        .append("\" stroke-width=\"4\" fill=\"none\" opacity=\".55\"/>");
            }
        }
        return s.toString();
    }

    private static String monstera(Plant p, Random r) {
        StringBuilder s = new StringBuilder();
        double[][] leaves = {{180, 300, -30, 1.0}, {420, 290, 25, 1.05}, {300, 200, -4, 1.15}, {230, 420, -55, .85},
            {380, 410, 50, .9}, {300, 360, 8, .95}};
        for (int i = 0; i < leaves.length; i++) {
            double[] l = leaves[i];
            String shade = i % 2 == 0 ? p.leaf() : darken(p.leaf(), .12);
            s.append("<path d=\"M300 556 Q").append(f((300 + l[0]) / 2)).append(" ").append(f((556 + l[1]) / 2 + 40))
                    .append(" ").append(f(l[0])).append(" ").append(f(l[1] + 60 * l[3])).append("\" stroke=\"")
                    .append(darken(p.leaf(), .2)).append("\" stroke-width=\"5\" fill=\"none\"/>");
            s.append("<g transform=\"translate(").append(f(l[0])).append(" ").append(f(l[1])).append(") rotate(")
                    .append(f(l[2])).append(") scale(").append(f(l[3])).append(")\">");
            s.append("<path d=\"M0 70 C-90 60 -100 -40 -40 -70 C-20 -80 -6 -70 0 -60 C6 -70 20 -80 40 -70 C100 -40 90 60 0 70 Z\" fill=\"")
                    .append(shade).append("\"/>");
            String hole = darken(shade, .38);
            s.append("<path d=\"M0 70 L0 -60\" stroke=\"").append(lighten(shade, .25)).append("\" stroke-width=\"3\"/>");
            for (int k = 0; k < 4; k++) {
                double y = -40 + k * 26;
                s.append("<path d=\"M-86 ").append(f(y + 8)).append(" L-30 ").append(f(y)).append("\" stroke=\"").append(hole)
                        .append("\" stroke-width=\"5\" stroke-linecap=\"round\"/>");
                s.append("<path d=\"M86 ").append(f(y + 8)).append(" L30 ").append(f(y)).append("\" stroke=\"").append(hole)
                        .append("\" stroke-width=\"5\" stroke-linecap=\"round\"/>");
            }
            s.append(circle(-22, -12, 7, hole)).append(circle(24, 18, 6, hole));
            s.append("</g>");
        }
        return s.toString();
    }

    private static String trailing(Plant p, Random r, boolean front) {
        StringBuilder s = new StringBuilder();
        String var = p.accent() == null ? lighten(p.leaf(), .35) : p.accent();
        int vines = front ? 3 : 6;
        for (int v = 0; v < vines; v++) {
            double sx = 230 + v * (140.0 / Math.max(1, vines - 1));
            double ex;
            double ey;
            if (front) {
                ex = sx + (r.nextDouble() - .5) * 120;
                ey = 640 + r.nextDouble() * 80;
            } else {
                ex = sx + (sx - 300) * 1.2 + (r.nextDouble() - .5) * 60;
                ey = 260 + r.nextDouble() * 120;
            }
            double sy = 556;
            double cx = (sx + ex) / 2 + (front ? (sx - 300) * .8 : 0);
            double cy = front ? 560 : (sy + ey) / 2 - 40;
            s.append("<path d=\"M").append(f(sx)).append(" ").append(f(sy)).append(" Q").append(f(cx)).append(" ").append(f(cy))
                    .append(" ").append(f(ex)).append(" ").append(f(ey)).append("\" stroke=\"").append(darken(p.leaf(), .25))
                    .append("\" stroke-width=\"3\" fill=\"none\"/>");
            for (int k = 1; k <= 7; k++) {
                double t = k / 7.0;
                double x = (1 - t) * (1 - t) * sx + 2 * (1 - t) * t * cx + t * t * ex;
                double y = (1 - t) * (1 - t) * sy + 2 * (1 - t) * t * cy + t * t * ey;
                double ang = (k % 2 == 0 ? 40 : -40) + (front ? 90 : -90) + (r.nextDouble() - .5) * 30;
                s.append(heart(x, y, 20 + r.nextDouble() * 8, ang, k % 3 == 0 ? darken(p.leaf(), .1) : p.leaf(), var));
            }
        }
        return s.toString();
    }

    private static String bonsai(Plant p, Random r) {
        StringBuilder s = new StringBuilder();
        String bark = "#5E4330";
        s.append("<path d=\"M282 612 C270 560 330 540 312 490 C298 450 240 450 250 400 C258 360 300 350 320 330 "
                + "L332 340 C316 362 276 372 272 404 C266 448 330 452 336 494 C344 548 296 566 312 612 Z\" fill=\"" + bark + "\"/>");
        s.append(trunk(bark, 300, 470, 380, 410, 6, 3)).append(trunk(bark, 262, 410, 190, 372, 5, 3));
        double[][] pads = {{326, 300, 92, 34}, {396, 392, 74, 28}, {188, 356, 70, 28}, {270, 250, 58, 24}};
        String[] shades = {darken(p.leaf(), .2), p.leaf(), lighten(p.leaf(), .15)};
        for (double[] pad : pads) {
            for (int layer = 0; layer < 3; layer++) {
                for (int k = 0; k < 6; k++) {
                    double x = pad[0] + (k - 2.5) * pad[2] / 3.2 + (r.nextDouble() - .5) * 10;
                    double y = pad[1] - layer * 7 + (r.nextDouble() - .5) * 8;
                    s.append(circle(x, y, pad[3] * (1 - layer * .2), shades[layer]));
                }
            }
        }
        return s.toString();
    }

    private static String column(Plant p, Random r) {
        StringBuilder s = new StringBuilder(trunk("#6B5038", 300, 560, 304, 120, 7, 4));
        String[] shades = {darken(p.leaf(), .1), p.leaf(), lighten(p.leaf(), .12)};
        for (int i = 0; i < 13; i++) {
            double y = 500 - i * 30;
            int side = i % 2 == 0 ? -1 : 1;
            double ang = side < 0 ? 200 + r.nextDouble() * 20 : -20 - r.nextDouble() * 20;
            double len = 64 - i * 1.5;
            double x = 302 + side * 6;
            String shade = shades[i % 3];
            double cx = x + Math.cos(Math.toRadians(ang)) * len * .6;
            double cy = y + Math.sin(Math.toRadians(ang)) * len * .6;
            s.append(leaf(cx, cy, len, len * .42, ang, shade));
            s.append("<path d=\"M").append(f(x)).append(" ").append(f(y)).append(" L")
                    .append(f(x + Math.cos(Math.toRadians(ang)) * len * 1.1)).append(" ")
                    .append(f(y + Math.sin(Math.toRadians(ang)) * len * 1.1)).append("\" stroke=\"").append(lighten(shade, .25))
                    .append("\" stroke-width=\"2\" opacity=\".7\"/>");
        }
        s.append(leaf(304, 108, 40, 16, -90, shades[2]));
        return s.toString();
    }

    // ------------------------------------------------------------------ shapes

    private static String leaf(double cx, double cy, double length, double width, double angle, String color) {
        return "<ellipse cx=\"0\" cy=\"0\" rx=\"" + f(length / 2) + "\" ry=\"" + f(width) + "\" fill=\"" + color
                + "\" transform=\"translate(" + f(cx) + " " + f(cy) + ") rotate(" + f(angle) + ") translate(" + f(length / 2)
                + " 0)\"/>";
    }

    private static String heart(double x, double y, double size, double angle, String color, String vein) {
        return "<g transform=\"translate(" + f(x) + " " + f(y) + ") rotate(" + f(angle) + ") scale(" + f(size / 20) + ")\">"
                + "<path d=\"M0 0 C-14 -4 -20 -20 -10 -28 C-4 -32 0 -28 0 -24 C0 -28 4 -32 10 -28 C20 -20 14 -4 0 0 Z\" fill=\""
                + color + "\"/><path d=\"M0 -2 L0 -22\" stroke=\"" + vein + "\" stroke-width=\"1.6\" opacity=\".7\"/></g>";
    }

    private static String leafSpeckles(Random r, double cx, double cy, double rx, double ry, String color, int n) {
        StringBuilder s = new StringBuilder("<g fill=\"" + color + "\" opacity=\".55\">");
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = Math.sqrt(r.nextDouble());
            double x = cx + Math.cos(a) * d * rx;
            double y = cy + Math.sin(a) * d * ry;
            s.append("<ellipse cx=\"").append(f(x)).append("\" cy=\"").append(f(y)).append("\" rx=\"7\" ry=\"3\" transform=\"rotate(")
                    .append(f(r.nextDouble() * 180)).append(" ").append(f(x)).append(" ").append(f(y)).append(")\"/>");
        }
        return s.append("</g>").toString();
    }

    private static String circle(double x, double y, double r, String fill) {
        return "<circle cx=\"" + f(x) + "\" cy=\"" + f(y) + "\" r=\"" + f(r) + "\" fill=\"" + fill + "\"/>";
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v).replace(".0", "");
    }

    // ------------------------------------------------------------------ colours

    static String darken(String hex, double amount) {
        return mix(hex, "#000000", amount);
    }

    static String lighten(String hex, double amount) {
        return mix(hex, "#FFFFFF", amount);
    }

    static String mix(String a, String b, double t) {
        int[] x = rgb(a);
        int[] y = rgb(b);
        return String.format("#%02X%02X%02X", Math.round(x[0] + (y[0] - x[0]) * t),
                Math.round(x[1] + (y[1] - x[1]) * t), Math.round(x[2] + (y[2] - x[2]) * t));
    }

    private static int[] rgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[] {Integer.parseInt(h.substring(0, 2), 16), Integer.parseInt(h.substring(2, 4), 16),
            Integer.parseInt(h.substring(4, 6), 16)};
    }
}
