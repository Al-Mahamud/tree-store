package com.shobuj.store.support;

import com.shobuj.store.shared.StoreProperties;
import com.shobuj.store.support.DemoArt.Form;
import com.shobuj.store.support.DemoArt.Plant;
import com.shobuj.store.support.DemoArt.Pot;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A believable nursery for local runs and tests: categories, collections and about thirty plants
 * with sizes, planters, stock and generated images. Deterministic, so tests can rely on it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DemoData {

    record Planter(String name, Pot kind, String hex, int[] surcharge) {
    }

    record Size(String size, String height, int price) {
    }

    record Item(String name, String localName, String botanical, String category, Plant look, String light,
                String water, String care, String placement, boolean petFriendly, String matureHeight,
                String growth, String fruiting, List<Size> sizes, List<Planter> planters, int salePercent,
                int ageDays, int sold, boolean featured, List<String> collections, String description,
                String details) {
    }

    static final Planter GROW_BAG = new Planter("Grow bag", Pot.GROW_BAG, "#262626", new int[] {0, 0, 0});
    static final Planter CLAY = new Planter("Clay pot", Pot.CLAY, "#B8643C", new int[] {250, 450, 700});
    static final Planter WHITE = new Planter("White ceramic", Pot.CERAMIC, "#EDEAE2", new int[] {450, 800, 1200});
    static final Planter FOREST = new Planter("Forest ceramic", Pot.CERAMIC, "#2F4A3A", new int[] {450, 800, 1200});
    static final Planter SAND = new Planter("Sand ceramic", Pot.CERAMIC, "#D9C9B0", new int[] {450, 800, 1200});
    static final Planter SLATE_TRAY = new Planter("Slate tray", Pot.CERAMIC, "#3B4A5A", new int[] {0, 0, 0});
    static final Planter RUST_TRAY = new Planter("Rust tray", Pot.CERAMIC, "#7A3B2E", new int[] {150, 250, 400});

    static final List<Planter> OUTDOOR = List.of(GROW_BAG, CLAY);
    static final List<Planter> INDOOR = List.of(WHITE, FOREST, CLAY);
    static final List<Planter> BONSAI = List.of(SLATE_TRAY, RUST_TRAY);

    static List<Size> sizes(int small, int medium, int large) {
        return List.of(new Size("SMALL", "1–2 ft", small), new Size("MEDIUM", "3–4 ft", medium),
                new Size("LARGE", "5–6 ft", large));
    }

    static List<Size> indoorSizes(int small, int medium, int large) {
        return List.of(new Size("SMALL", "30–45 cm", small), new Size("MEDIUM", "60–75 cm", medium),
                new Size("LARGE", "90–120 cm", large));
    }

    static Plant look(Form form, String leaf, String accent, long seed) {
        return new Plant(form, leaf, accent, seed);
    }

    static final List<Item> ITEMS = List.of(
            // Fruit trees
            new Item("Amrapali Mango", "আম্রপালি আম", "Mangifera indica 'Amrapali'", "fruit-trees",
                    look(Form.FRUIT_TREE, "#3F7A3A", "#F2A93B", 1), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "4–6 m", "MEDIUM", "Fruits in 2–3 years", sizes(450, 950, 2400), OUTDOOR, 0, 4, 412, true,
                    List.of("monsoon-planting", "rooftop-garden"),
                    "A grafted dwarf mango that stays compact enough for a rooftop drum yet still gives sweet, fibre-free fruit in early summer. Our most-loved tree for first-time growers.",
                    "Grafted, so it fruits years sooner than seed-grown mango\nCompact canopy, ideal for rooftops\nSweet, fibreless flesh\nHardened in full sun before dispatch"),
            new Item("Kagzi Lemon", "কাগজি লেবু", "Citrus aurantiifolia", "fruit-trees",
                    look(Form.FRUIT_TREE, "#4C8A3E", "#E8D44A", 2), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "2–3 m", "FAST", "Fruits in 1–2 years", sizes(350, 750, 1600), OUTDOOR, 0, 40, 380, true,
                    List.of("rooftop-garden"),
                    "Thin-skinned, fragrant and generous — a kagzi lemon tree keeps the kitchen supplied almost all year. Happy in a 14-inch pot on a sunny balcony.",
                    "Fruits several times a year\nThin, aromatic skin\nThrives in pots and drums\nGrafted from a heavy-bearing mother plant"),
            new Item("Thai Guava", "থাই পেয়ারা", "Psidium guajava", "fruit-trees",
                    look(Form.FRUIT_TREE, "#4A8C42", "#B7D36B", 3), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "3–4 m", "FAST", "Fruits in 1 year", sizes(300, 650, 1500), OUTDOOR, 15, 60, 290, false,
                    List.of("monsoon-planting", "rooftop-garden"),
                    "Crunchy, large, almost seedless guavas from a tree that starts bearing in its first year. Tough, forgiving and fast.",
                    "Large, crisp fruit with few seeds\nBears from the first year\nTolerates heat and heavy rain"),
            new Item("Bombai Lychee", "বোম্বাই লিচু", "Litchi chinensis", "fruit-trees",
                    look(Form.FRUIT_TREE, "#2F6B35", "#C8344A", 4), "FULL_SUN", "HIGH", "MODERATE", "OUTDOOR", false,
                    "6–8 m", "SLOW", "Fruits in 3–4 years", sizes(550, 1200, 2900), List.of(GROW_BAG), 0, 25, 156, false,
                    List.of("monsoon-planting"),
                    "The famous juicy lychee of Dinajpur, grown from air-layered stock so it carries the true flavour of its parent.",
                    "Air-layered, true to type\nSweet, juicy fruit in May–June\nBest planted in the ground"),
            new Item("Malta Orange", "মাল্টা", "Citrus sinensis", "fruit-trees",
                    look(Form.FRUIT_TREE, "#3E7B3A", "#F08A24", 5), "FULL_SUN", "MEDIUM", "MODERATE", "OUTDOOR", false,
                    "3–4 m", "MEDIUM", "Fruits in 2 years", sizes(400, 850, 1900), OUTDOOR, 0, 8, 201, false,
                    List.of("rooftop-garden"),
                    "Green-skinned, juicy BARI Malta-1, bred to fruit well in Bangladesh's heat.",
                    "BARI Malta-1 variety\nJuicy, sweet-sharp fruit\nGood for large pots"),
            new Item("Pomegranate", "ডালিম", "Punica granatum", "fruit-trees",
                    look(Form.FRUIT_TREE, "#4F8740", "#B3263A", 6), "FULL_SUN", "LOW", "EASY", "OUTDOOR", false,
                    "2–3 m", "MEDIUM", "Fruits in 2 years", sizes(380, 800, 1800), OUTDOOR, 0, 75, 133, false,
                    List.of("rooftop-garden"),
                    "Orange-red flowers followed by ruby fruit. Drought tolerant and happy in a sunny pot.",
                    "Showy flowers before the fruit\nNeeds little water once settled\nCompact, shrubby habit"),
            new Item("Khirsapat Jackfruit", "কাঁঠাল", "Artocarpus heterophyllus", "fruit-trees",
                    look(Form.FRUIT_TREE, "#2E6A35", "#9DBB4A", 7), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "10–15 m", "MEDIUM", "Fruits in 4–5 years", sizes(300, 700, 1800), List.of(GROW_BAG), 0, 90, 98, false,
                    List.of("monsoon-planting"),
                    "Our national fruit tree, grafted for earlier, sweeter crops. Plant it in the ground where it has room to grow.",
                    "Grafted for earlier fruiting\nFirm, sweet bulbs\nNeeds open ground, not a pot"),
            new Item("Coconut Palm", "নারিকেল", "Cocos nucifera (dwarf)", "palms",
                    look(Form.PALM, "#5C9A45", null, 8), "FULL_SUN", "HIGH", "EASY", "OUTDOOR", false,
                    "6–8 m", "MEDIUM", "Fruits in 3–4 years", sizes(500, 1100, 2600), List.of(GROW_BAG), 10, 55, 144, false,
                    List.of("monsoon-planting"),
                    "A dwarf hybrid coconut that bears years earlier than local tall varieties and stays easier to harvest.",
                    "Dwarf hybrid, early bearing\nSweet coconut water\nPlant in open ground"),
            // Flowering trees
            new Item("Krishnachura", "কৃষ্ণচূড়া", "Delonix regia", "flowering-trees",
                    look(Form.FLOWER_TREE, "#467E3C", "#E4572E", 9), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "8–12 m", "FAST", null, sizes(350, 750, 1800), List.of(GROW_BAG), 0, 3, 188, true,
                    List.of("monsoon-planting"),
                    "The flame tree that sets Bangladesh alight every Boishakh. A wide, shady canopy covered in scarlet flowers.",
                    "Brilliant red-orange flowers in April–June\nWide, shady canopy\nPlant in open ground"),
            new Item("Radhachura", "রাধাচূড়া", "Caesalpinia pulcherrima", "flowering-trees",
                    look(Form.FLOWER_TREE, "#4C8A3E", "#F2B630", 10), "FULL_SUN", "LOW", "EASY", "BOTH", false,
                    "2–3 m", "FAST", null, sizes(250, 550, 1300), OUTDOOR, 0, 33, 121, false,
                    List.of("rooftop-garden"),
                    "Feathery leaves and fiery yellow-orange flowers for most of the year. Small enough for a big pot.",
                    "Flowers nearly all year\nHandles heat and drought\nGood in large pots"),
            new Item("Bougainvillea", "বাগানবিলাস", "Bougainvillea glabra", "flowering-trees",
                    look(Form.FLOWER_TREE, "#4A803E", "#C2298A", 11), "FULL_SUN", "LOW", "EASY", "OUTDOOR", false,
                    "2–5 m", "FAST", null, sizes(280, 600, 1400), OUTDOOR, 20, 45, 265, false,
                    List.of("rooftop-garden"),
                    "Papery magenta bracts that cover walls and railings in colour. The less you water it, the more it flowers.",
                    "Magenta bracts most of the year\nTrain it on a wall, railing or as a standard\nThrives on neglect"),
            new Item("Red Hibiscus", "জবা", "Hibiscus rosa-sinensis", "flowering-trees",
                    look(Form.FLOWER_TREE, "#3F7A3A", "#D7263D", 12), "FULL_SUN", "MEDIUM", "EASY", "BOTH", false,
                    "1.5–2.5 m", "FAST", null, sizes(220, 480, 1100), List.of(GROW_BAG, CLAY, WHITE), 0, 12, 230, false,
                    List.of("rooftop-garden", "gifts-that-grow"),
                    "Big, glossy red flowers almost every day. A classic for balconies and puja gardens.",
                    "Flowers year round in sun\nEasy to prune into shape\nAttracts butterflies"),
            new Item("Beli Jasmine", "বেলি ফুল", "Jasminum sambac", "flowering-trees",
                    look(Form.FLOWER_TREE, "#3A7438", "#F7F4EA", 13), "FULL_SUN", "MEDIUM", "EASY", "BOTH", true,
                    "1–2 m", "MEDIUM", null, sizes(200, 420, 950), List.of(GROW_BAG, CLAY, WHITE), 0, 6, 175, false,
                    List.of("gifts-that-grow"),
                    "Small white flowers with the sweetest evening scent. Keep it by a window or door to enjoy it every night.",
                    "Intensely fragrant flowers\nCompact and bushy\nSafe around cats and dogs"),
            // Shade & timber
            new Item("Neem", "নিম", "Azadirachta indica", "shade-timber",
                    look(Form.SAPLING, "#4F8B3F", null, 14), "FULL_SUN", "LOW", "EASY", "OUTDOOR", false,
                    "12–15 m", "FAST", null, sizes(150, 350, 900), List.of(GROW_BAG), 0, 80, 220, false,
                    List.of("monsoon-planting"),
                    "The village pharmacy: a hardy, fast-growing shade tree with medicinal leaves that keeps insects away.",
                    "Medicinal leaves and bark\nCool, dense shade\nThrives in poor soil"),
            new Item("Mahogany", "মেহগনি", "Swietenia macrophylla", "shade-timber",
                    look(Form.SAPLING, "#3D7A3A", null, 15), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "20–25 m", "FAST", null, sizes(120, 280, 750), List.of(GROW_BAG), 0, 110, 340, false,
                    List.of("monsoon-planting"),
                    "Valuable timber and generous shade. A long-term investment for village land, roadsides and boundaries.",
                    "Valuable hardwood timber\nStraight, fast growth\nIdeal for boundaries and roadsides"),
            new Item("Arjun", "অর্জুন", "Terminalia arjuna", "shade-timber",
                    look(Form.SAPLING, "#4A863E", null, 16), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "20–25 m", "MEDIUM", null, sizes(140, 320, 820), List.of(GROW_BAG), 0, 65, 87, false,
                    List.of("monsoon-planting"),
                    "A riverside giant prized for its medicinal bark and its tolerance of waterlogged ground.",
                    "Medicinal bark\nTolerates flooding\nWide, spreading crown"),
            new Item("Segun Teak", "সেগুন", "Tectona grandis", "shade-timber",
                    look(Form.SAPLING, "#5A8F45", null, 17), "FULL_SUN", "MEDIUM", "MODERATE", "OUTDOOR", false,
                    "25–30 m", "MEDIUM", null, sizes(160, 380, 950), List.of(GROW_BAG), 0, 140, 63, false,
                    List.of(),
                    "The finest furniture timber, grown from selected clonal stock for straight trunks.",
                    "Premium timber\nClonal stock for straight trunks\nPlant in well-drained ground"),
            // Palms
            new Item("Areca Palm", "সুপারি পাম", "Dypsis lutescens", "palms",
                    look(Form.PALM, "#6AA54C", null, 18), "BRIGHT", "MEDIUM", "EASY", "BOTH", true,
                    "2–3 m indoors", "MEDIUM", null, indoorSizes(450, 1100, 2400), INDOOR, 0, 18, 305, true,
                    List.of("gifts-that-grow"),
                    "Feathery, arching fronds that soften any corner. One of the best palms for cleaning indoor air, and safe for pets.",
                    "Air-purifying\nSafe for cats and dogs\nGrows several stems for a full look"),
            new Item("Royal Palm", "রয়্যাল পাম", "Roystonea regia", "palms",
                    look(Form.PALM, "#4F8E40", null, 19), "FULL_SUN", "MEDIUM", "EASY", "OUTDOOR", false,
                    "15–20 m", "MEDIUM", null, sizes(400, 900, 2200), List.of(GROW_BAG), 0, 70, 54, false,
                    List.of(),
                    "Smooth grey trunks and a crown of graceful fronds: the classic avenue palm for driveways and boundaries.",
                    "Elegant smooth trunk\nPerfect for avenues and gates\nPlant in open ground"),
            // Indoor plants
            new Item("Snake Plant", "স্নেক প্ল্যান্ট", "Dracaena trifasciata", "indoor-easy",
                    look(Form.SNAKE, "#2F6B3F", "#D8D27A", 20), "LOW", "LOW", "EASY", "INDOOR", false,
                    "60–90 cm", "SLOW", null, indoorSizes(350, 750, 1500), INDOOR, 0, 22, 520, true,
                    List.of("gifts-that-grow"),
                    "Practically unkillable. Upright, gold-edged leaves that cope with dim corners, air conditioning and forgotten watering.",
                    "Tolerates low light\nWater every 2–3 weeks\nReleases oxygen at night"),
            new Item("ZZ Plant", "জেডজেড প্ল্যান্ট", "Zamioculcas zamiifolia", "indoor-easy",
                    look(Form.COLUMN, "#1F5A30", null, 21), "LOW", "LOW", "EASY", "INDOOR", false,
                    "60–90 cm", "SLOW", null, indoorSizes(450, 900, 1800), INDOOR, 0, 5, 210, false,
                    List.of("gifts-that-grow"),
                    "Glossy, waxy leaves that look polished all year — even in an office with no window.",
                    "Survives low light and drought\nGlossy, architectural leaves\nIdeal desk or office plant"),
            new Item("Money Plant", "মানিপ্ল্যান্ট", "Epipremnum aureum", "indoor-trailing",
                    look(Form.TRAILING, "#4E9A48", "#E9E2A0", 22), "MEDIUM", "MEDIUM", "EASY", "INDOOR", false,
                    "Trails 2 m+", "FAST", null, indoorSizes(180, 380, 750), List.of(WHITE, SAND, CLAY), 0, 35, 610, false,
                    List.of("gifts-that-grow"),
                    "Golden-splashed heart leaves that trail from shelves or climb a moss pole. Believed to bring good luck.",
                    "Trails or climbs\nGrows in water or soil\nEasy to propagate"),
            new Item("Monstera Deliciosa", "মনস্টেরা", "Monstera deliciosa", "indoor-statement",
                    look(Form.MONSTERA, "#2E6B3F", null, 23), "BRIGHT", "MEDIUM", "MODERATE", "INDOOR", false,
                    "1.5–2 m", "MEDIUM", null, indoorSizes(650, 1400, 3200), INDOOR, 10, 10, 245, true,
                    List.of("gifts-that-grow"),
                    "The split-leaf statement plant. Big, glossy leaves develop their famous holes as the plant matures.",
                    "Dramatic split leaves\nGive it bright, indirect light\nAdd a moss pole as it grows"),
            new Item("Fiddle Leaf Fig", "ফিডল লিফ ফিগ", "Ficus lyrata", "indoor-statement",
                    look(Form.COLUMN, "#2F6A35", null, 24), "BRIGHT", "MEDIUM", "EXPERT", "INDOOR", false,
                    "1.5–2 m", "MEDIUM", null, indoorSizes(750, 1600, 3500), INDOOR, 0, 2, 92, false,
                    List.of(),
                    "Large violin-shaped leaves on an upright stem. A designer favourite that rewards a bright, steady spot.",
                    "Huge, violin-shaped leaves\nNeeds bright light and a fixed spot\nDislikes being moved"),
            new Item("Rubber Plant", "রাবার প্ল্যান্ট", "Ficus elastica 'Burgundy'", "indoor-statement",
                    look(Form.COLUMN, "#24402E", null, 25), "MEDIUM", "LOW", "EASY", "INDOOR", false,
                    "1–2 m", "MEDIUM", null, indoorSizes(400, 900, 2000), INDOOR, 0, 50, 168, false,
                    List.of(),
                    "Deep burgundy, almost black leaves with a rubbery shine. Bold, forgiving and quick to fill a corner.",
                    "Dark, glossy leaves\nForgiving of missed waterings\nWipe leaves to keep them shiny"),
            // Bonsai
            new Item("Ficus Bonsai", "ফাইকাস বনসাই", "Ficus microcarpa", "bonsai",
                    look(Form.BONSAI, "#3E7438", null, 26), "BRIGHT", "MEDIUM", "MODERATE", "BOTH", false,
                    "30–45 cm", "SLOW", null,
                    List.of(new Size("SMALL", "5 years old", 1800), new Size("MEDIUM", "8 years old", 3500),
                            new Size("LARGE", "12 years old", 7500)), BONSAI, 0, 15, 74, true,
                    List.of("gifts-that-grow"),
                    "A living sculpture with an aerial-root trunk, shaped by hand in our nursery. The most forgiving bonsai to start with.",
                    "Hand-shaped trunk and canopy\nCan live indoors near a bright window\nComes with a care card"),
            new Item("Bougainvillea Bonsai", "বাগানবিলাস বনসাই", "Bougainvillea spectabilis", "bonsai",
                    look(Form.BONSAI, "#477F3D", null, 27), "FULL_SUN", "LOW", "EXPERT", "OUTDOOR", false,
                    "30–50 cm", "SLOW", null,
                    List.of(new Size("SMALL", "4 years old", 2200), new Size("MEDIUM", "7 years old", 4500),
                            new Size("LARGE", "10 years old", 9000)), BONSAI, 0, 120, 31, false,
                    List.of(),
                    "Thick, twisted trunk crowned with magenta flowers. A showpiece for a sunny balcony.",
                    "Flowering bonsai\nNeeds full sun outdoors\nFor experienced growers")
    );

    /** A demo customer for local runs. */
    public static final String DEMO_PHONE = "01755555555";
    public static final String DEMO_PASSWORD = "shobuj-demo";

    /** A demo staff account for the admin area. */
    public static final String STAFF_PHONE = "01999999999";
    public static final String STAFF_PASSWORD = "shobuj-admin-1";

    /** Days ago, then slug / size / quantity triples. */
    static final List<Object[]> DEMO_ORDERS = List.of(
            new Object[] {12, "amrapali-mango", "SMALL", 2, "snake-plant", "SMALL", 1},
            new Object[] {40, "money-plant", "MEDIUM", 1, "neem", "SMALL", 1});

    static final List<String> DEMO_WISHLIST = List.of("monstera-deliciosa", "ficus-bonsai", "krishnachura");

    @Bean
    ApplicationRunner demoDataLoader(JdbcClient jdbc, TransactionTemplate tx, StoreProperties properties,
                                     org.springframework.security.crypto.password.PasswordEncoder passwords) {
        return new Loader(jdbc, tx, properties.storageRoot().resolve("demo"), passwords);
    }

    static final class Loader implements ApplicationRunner {

        private final JdbcClient jdbc;
        private final TransactionTemplate tx;
        private final Path folder;
        private final Map<String, Long> categories = new HashMap<>();
        private final Map<String, Long> collections = new HashMap<>();

        private final org.springframework.security.crypto.password.PasswordEncoder passwords;

        Loader(JdbcClient jdbc, TransactionTemplate tx, Path folder,
               org.springframework.security.crypto.password.PasswordEncoder passwords) {
            this.passwords = passwords;
            this.jdbc = jdbc;
            this.tx = tx;
            this.folder = folder;
        }

        @Override
        public void run(ApplicationArguments args) {
            if (jdbc.sql("select count(*) from products").query(Long.class).single() == 0) {
                tx.executeWithoutResult(status -> load());
            }
        }

        private void load() {
            category(null, "Trees", "trees", "Fruit, flowering, shade and timber trees for gardens, rooftops and land.", 0);
            category("trees", "Fruit trees", "fruit-trees", "Grafted trees that fruit years sooner.", 0);
            category("trees", "Flowering trees", "flowering-trees", "Colour and scent for every season.", 1);
            category("trees", "Shade & timber", "shade-timber", "Long-lived trees for land and boundaries.", 2);
            category("trees", "Palms", "palms", "Palms for avenues, gardens and living rooms.", 3);
            category(null, "Indoor plants", "indoor", "Greenery that copes with flats, offices and air conditioning.", 1);
            category("indoor", "Easy care", "indoor-easy", "Nearly unkillable plants for busy people.", 0);
            category("indoor", "Statement plants", "indoor-statement", "Big leaves that fill a corner.", 1);
            category("indoor", "Trailing & hanging", "indoor-trailing", "For shelves, hooks and moss poles.", 2);
            category(null, "Bonsai", "bonsai", "Hand-shaped miniature trees, years in the making.", 2);

            Plant mango = ITEMS.get(0).look();
            collection("Monsoon Planting", "monsoon-planting", "Plant now, harvest for decades",
                    "June to September is the best time to put trees in the ground. Fruit, timber and shade trees, hardened and ready.",
                    0, DemoArt.banner("#B9C4A6",
                            new Object[] {ITEMS.get(14).look(), Pot.GROW_BAG, GROW_BAG.hex()},
                            new Object[] {mango, Pot.CLAY, CLAY.hex()},
                            new Object[] {ITEMS.get(8).look(), Pot.GROW_BAG, GROW_BAG.hex()}));
            collection("Gifts that Grow", "gifts-that-grow", "Presents that outlast the flowers",
                    "Easy-care plants and bonsai in ceramic pots, ready to give for birthdays, housewarmings and Eid.",
                    1, DemoArt.banner("#D8CBB5",
                            new Object[] {ITEMS.get(19).look(), Pot.CERAMIC, WHITE.hex()},
                            new Object[] {ITEMS.get(25).look(), Pot.CERAMIC, SLATE_TRAY.hex()},
                            new Object[] {ITEMS.get(21).look(), Pot.CERAMIC, SAND.hex()}));
            collection("Rooftop Garden", "rooftop-garden", "A harvest above the city",
                    "Compact fruit trees and sun-loving flowers that thrive in drums and big pots on Dhaka rooftops.",
                    2, DemoArt.banner("#C9B98F",
                            new Object[] {ITEMS.get(1).look(), Pot.CLAY, CLAY.hex()},
                            new Object[] {ITEMS.get(10).look(), Pot.CLAY, CLAY.hex()}));

            coupon("GREEN10", "10% off your first plants", "PERCENT", 10, 500, 1000, null);
            coupon("MONSOON300", "৳300 off orders over ৳3,000", "FIXED", 300, null, 3000, null);
            coupon("FREEDELIVERY", "Free delivery on orders over ৳2,000", "FREE_SHIPPING", 0, null, 2000, null);
            coupon("SPRING25", "Last spring's sale", "PERCENT", 25, null, 0, Instant.now().minus(Duration.ofDays(30)));

            Instant now = Instant.now();
            for (int i = 0; i < ITEMS.size(); i++) {
                product(ITEMS.get(i), i, now);
            }
            jdbc.sql("""
                    update categories c set image_url = (
                        select pi.url from products p
                          join product_images pi on pi.product_id = p.id
                          left join planters pl on pl.id = pi.planter_id
                         where p.category_id in (select id from categories where id = c.id or parent_id = c.id)
                         order by p.featured desc, p.sold_count desc, pl.position, pi.position limit 1)
                    """).update();
            demoCustomer(now);
            jdbc.sql("insert into staff (name, phone, password_hash) values ('Farhana (nursery manager)', :p, :h)")
                    .param("p", STAFF_PHONE).param("h", passwords.encode(STAFF_PASSWORD)).update();
            demoQueue(now);
        }

        /**
         * Guest orders from the last few days for the admin area: hours ago, name, phone, address, area,
         * city, zone, status, payment method, then slug / size / quantity triples.
         */
        static final List<Object[]> QUEUE = List.of(
                new Object[] {3, "Tanvir Hasan", "01712000101", "House 7, Road 3, Sector 11", "Uttara", "Dhaka", "DHAKA",
                        "PLACED", "COD", "amrapali-mango", "MEDIUM", 1, "kagzi-lemon", "SMALL", 2},
                new Object[] {9, "Sharmin Akter", "01812000202", "Flat 4B, 22 Lake Circus", "Kalabagan", "Dhaka", "DHAKA",
                        "PLACED", "ONLINE", "monstera-deliciosa", "MEDIUM", 1},
                new Object[] {20, "Rafiq Ullah", "01912000303", "Shapla Villa, College Road", "Kotwali", "Chattogram", "OUTSIDE",
                        "CONFIRMED", "COD", "snake-plant", "SMALL", 3, "money-plant", "SMALL", 2},
                new Object[] {30, "Mehnaz Chowdhury", "01612000404", "House 51, Road 27", "Banani", "Dhaka", "DHAKA",
                        "CONFIRMED", "ONLINE", "krishnachura", "LARGE", 1},
                new Object[] {52, "Arif Mahmud", "01512000505", "Holding 12, Station Road", "Sadar", "Rajshahi", "OUTSIDE",
                        "SHIPPED", "COD", "neem", "SMALL", 4},
                new Object[] {75, "Lubna Sultana", "01312000606", "Flat 9A, Concord Tower", "Mohakhali", "Dhaka", "DHAKA",
                        "DELIVERED", "ONLINE", "ficus-bonsai", "SMALL", 1, "snake-plant", "MEDIUM", 1},
                new Object[] {100, "Kamal Hossain", "01712000707", "Road 2, Block C", "Mirpur 10", "Dhaka", "DHAKA",
                        "DELIVERED", "COD", "amrapali-mango", "SMALL", 3},
                new Object[] {130, "Nusrat Jahan", "01812000808", "House 3, Ambagan", "Sylhet Sadar", "Sylhet", "OUTSIDE",
                        "DELIVERED", "COD", "money-plant", "MEDIUM", 2});

        private void demoQueue(Instant now) {
            List<String> flow = List.of("PLACED", "CONFIRMED", "SHIPPED", "DELIVERED");
            for (Object[] o : QUEUE) {
                Instant placed = now.minus(Duration.ofHours((Integer) o[0]));
                String status = (String) o[7];
                String method = (String) o[8];
                boolean paid = method.equals("ONLINE") || status.equals("DELIVERED");
                String number = "SB" + jdbc.sql("select nextval('order_number_seq')").query(Long.class).single();
                long order = jdbc.sql("""
                                insert into orders (number, access_token, name, phone, address, area, city, zone,
                                                    subtotal, shipping_fee, total, payment_method, payment_status, paid_at,
                                                    status, courier, tracking_code, created_at, updated_at)
                                values (:n, md5(random()::text), :name, :phone, :address, :area, :city, :zone, 0, 0, 0,
                                        :method, :paid, :paidAt, :status, :courier, :tracking, :at, :at)
                                returning id""")
                        .param("n", number).param("name", o[1]).param("phone", o[2]).param("address", o[3])
                        .param("area", o[4]).param("city", o[5]).param("zone", o[6]).param("method", method)
                        .param("paid", paid ? "PAID" : "UNPAID").param("paidAt", paid ? Timestamp.from(placed) : null)
                        .param("status", status)
                        .param("courier", o[6].equals("OUTSIDE") && flow.indexOf(status) >= 2 ? "Sundarban Courier" : null)
                        .param("tracking", o[6].equals("OUTSIDE") && flow.indexOf(status) >= 2 ? "SCS-48" + o[0] + "17" : null)
                        .param("at", Timestamp.from(placed))
                        .query(Long.class).single();
                for (int i = 9; i < o.length; i += 3) {
                    jdbc.sql("""
                                    insert into order_items (order_id, variant_id, product_id, product_slug, product_name, planter,
                                                             size, height, sku, image_url, unit_price, quantity)
                                    select :o, v.id, p.id, p.slug, p.name, pl.name, :sizeLabel, v.height, v.sku,
                                           (select url from product_images where product_id = p.id order by position limit 1),
                                           v.price, :q
                                      from variants v join products p on p.id = v.product_id join planters pl on pl.id = v.planter_id
                                     where p.slug = :slug and v.size = :size
                                     order by v.stock > 0 desc, pl.position limit 1""")
                            .param("o", order).param("slug", o[i]).param("size", o[i + 1])
                            .param("sizeLabel", com.shobuj.store.catalog.Care.Size.valueOf((String) o[i + 1]).label)
                            .param("q", o[i + 2]).update();
                }
                int fee = o[6].equals("DHAKA") ? 100 : 200;
                jdbc.sql("""
                                update orders o set subtotal = s.sum, total = s.sum + case when s.sum >= 5000 then 0 else :fee end,
                                                    shipping_fee = case when s.sum >= 5000 then 0 else :fee end
                                  from (select sum(unit_price * quantity) as sum from order_items where order_id = :o) s
                                 where o.id = :o""")
                        .param("o", order).param("fee", fee).update();
                for (int step = 0; step <= flow.indexOf(status); step++) {
                    jdbc.sql("insert into order_events (order_id, status, actor, created_at) values (:o, :s, :a, :at)")
                            .param("o", order).param("s", flow.get(step))
                            .param("a", step == 0 ? "customer" : "staff:Farhana (nursery manager)")
                            .param("at", Timestamp.from(placed.plus(Duration.ofHours(step * 2L)))).update();
                }
            }
        }

        /** A customer with a saved address, two delivered orders and saved plants. */
        private void demoCustomer(Instant now) {
            long customer = jdbc.sql("""
                            insert into customers (name, phone, password_hash, phone_verified_at)
                            values ('Nadia Rahman', :phone, :hash, now()) returning id""")
                    .param("phone", DEMO_PHONE).param("hash", passwords.encode(DEMO_PASSWORD)).query(Long.class).single();
            jdbc.sql("""
                            insert into addresses (customer_id, label, name, phone, address, area, city, zone, is_default)
                            values (:c, 'Home', 'Nadia Rahman', :phone, 'House 14, Road 9A, Flat 5C', 'Dhanmondi', 'Dhaka', 'DHAKA', true)""")
                    .param("c", customer).param("phone", DEMO_PHONE).update();
            for (Object[] o : DEMO_ORDERS) {
                Instant placed = now.minus(Duration.ofDays((Integer) o[0]));
                String number = "SB" + jdbc.sql("select nextval('order_number_seq')").query(Long.class).single();
                long order = jdbc.sql("""
                                insert into orders (number, access_token, customer_id, name, phone, address, area, city, zone,
                                                    subtotal, shipping_fee, total, payment_method, payment_status, status,
                                                    created_at, updated_at)
                                values (:n, md5(random()::text), :c, 'Nadia Rahman', :phone, 'House 14, Road 9A, Flat 5C',
                                        'Dhanmondi', 'Dhaka', 'DHAKA', 0, 0, 0, 'COD', 'PAID', 'DELIVERED', :at, :at)
                                returning id""")
                        .param("n", number).param("c", customer).param("phone", DEMO_PHONE).param("at", java.sql.Timestamp.from(placed))
                        .query(Long.class).single();
                for (int i = 1; i < o.length; i += 3) {
                    jdbc.sql("""
                                    insert into order_items (order_id, variant_id, product_id, product_slug, product_name, planter,
                                                             size, height, sku, image_url, unit_price, quantity)
                                    select :o, v.id, p.id, p.slug, p.name, pl.name, :sizeLabel, v.height, v.sku,
                                           (select url from product_images where product_id = p.id order by position limit 1),
                                           v.price, :q
                                      from variants v join products p on p.id = v.product_id join planters pl on pl.id = v.planter_id
                                     where p.slug = :slug and v.size = :size
                                     order by v.stock > 0 desc, pl.position limit 1""")
                            .param("o", order).param("slug", o[i]).param("size", o[i + 1])
                            .param("sizeLabel", com.shobuj.store.catalog.Care.Size.valueOf((String) o[i + 1]).label)
                            .param("q", o[i + 2]).update();
                }
                jdbc.sql("""
                                update orders o set subtotal = s.sum, total = s.sum + case when s.sum >= 5000 then 0 else 100 end,
                                                    shipping_fee = case when s.sum >= 5000 then 0 else 100 end
                                  from (select sum(unit_price * quantity) as sum from order_items where order_id = :o) s
                                 where o.id = :o""")
                        .param("o", order).update();
                jdbc.sql("insert into order_events (order_id, status, actor, created_at) values (:o, 'PLACED', 'customer', :at)")
                        .param("o", order).param("at", java.sql.Timestamp.from(placed)).update();
                jdbc.sql("insert into order_events (order_id, status, actor, created_at) values (:o, 'DELIVERED', 'shop', :at)")
                        .param("o", order).param("at", java.sql.Timestamp.from(placed.plus(Duration.ofDays(2)))).update();
            }
            for (String slug : DEMO_WISHLIST) {
                jdbc.sql("insert into wishlist_items (customer_id, product_id) select :c, id from products where slug = :s")
                        .param("c", customer).param("s", slug).update();
            }
        }

        private void coupon(String code, String description, String kind, int value, Integer max, int min, Instant ends) {
            jdbc.sql("""
                            insert into coupons (code, description, kind, value, max_discount, min_subtotal, ends_at)
                            values (:code, :description, :kind, :value, :max, :min, :ends)""")
                    .param("code", code).param("description", description).param("kind", kind).param("value", value)
                    .param("max", max).param("min", min).param("ends", ends == null ? null : Timestamp.from(ends))
                    .update();
        }

        private void category(String parent, String name, String slug, String description, int position) {
            long id = jdbc.sql("""
                            insert into categories (parent_id, name, slug, description, position)
                            values (:parent, :name, :slug, :description, :position) returning id""")
                    .param("parent", parent == null ? null : categories.get(parent))
                    .param("name", name).param("slug", slug).param("description", description)
                    .param("position", position)
                    .query(Long.class).single();
            categories.put(slug, id);
        }

        private void collection(String name, String slug, String tagline, String description, int position, String svg) {
            String url = write("collection-" + slug + ".svg", svg);
            long id = jdbc.sql("""
                            insert into collections (name, slug, tagline, description, image_url, position)
                            values (:name, :slug, :tagline, :description, :url, :position) returning id""")
                    .param("name", name).param("slug", slug).param("tagline", tagline)
                    .param("description", description).param("url", url).param("position", position)
                    .query(Long.class).single();
            collections.put(slug, id);
        }

        private void product(Item p, int index, Instant now) {
            String slug = slug(p.name());
            Instant created = now.minus(Duration.ofDays(p.ageDays())).minus(Duration.ofMinutes(index));
            long id = jdbc.sql("""
                            insert into products (name, local_name, botanical_name, slug, category_id, description,
                                                  details, light, water, care_level, placement, pet_friendly,
                                                  mature_height, growth_rate, fruiting, status, featured, sold_count,
                                                  created_at, updated_at)
                            values (:name, :local, :botanical, :slug, :category, :description, :details, :light,
                                    :water, :care, :placement, :pet, :height, :growth, :fruiting, 'ACTIVE',
                                    :featured, :sold, :created, :created)
                            returning id""")
                    .param("name", p.name()).param("local", p.localName()).param("botanical", p.botanical())
                    .param("slug", slug).param("category", categories.get(p.category()))
                    .param("description", p.description()).param("details", p.details())
                    .param("light", p.light()).param("water", p.water()).param("care", p.care())
                    .param("placement", p.placement()).param("pet", p.petFriendly())
                    .param("height", p.matureHeight()).param("growth", p.growth()).param("fruiting", p.fruiting())
                    .param("featured", p.featured()).param("sold", p.sold()).param("created", Timestamp.from(created))
                    .query(Long.class).single();
            for (String collection : p.collections()) {
                jdbc.sql("insert into product_collections (product_id, collection_id) values (:p, :c)")
                        .param("p", id).param("c", collections.get(collection)).update();
            }
            boolean outdoorTree = p.category().equals("fruit-trees") || p.category().equals("flowering-trees")
                    || p.category().equals("shade-timber") || (p.category().equals("palms") && p.placement().equals("OUTDOOR"));
            for (int pi = 0; pi < p.planters().size(); pi++) {
                Planter planter = p.planters().get(pi);
                long planterId = jdbc.sql("""
                                insert into planters (product_id, name, kind, hex, position)
                                values (:p, :name, :kind, :hex, :position) returning id""")
                        .param("p", id).param("name", planter.name()).param("kind", planter.kind().name())
                        .param("hex", planter.hex()).param("position", pi)
                        .query(Long.class).single();
                String base = slug + "-" + slug(planter.name());
                image(id, planterId, write(base + "-1.svg", DemoArt.product(p.look(), planter.kind(), planter.hex())),
                        p.name() + " in a " + planter.name().toLowerCase(Locale.ROOT), 0);
                image(id, planterId, write(base + "-2.svg", DemoArt.detail(p.look(), planter.kind(), planter.hex())),
                        p.name() + ", close-up of the leaves", 1);
                for (int si = 0; si < p.sizes().size(); si++) {
                    Size size = p.sizes().get(si);
                    int price = round10(size.price() + planter.surcharge()[si]);
                    Integer compareAt = p.salePercent() > 0 ? round10(price * 100 / (100 - p.salePercent())) : null;
                    if (compareAt != null && compareAt <= price) {
                        compareAt = null;
                    }
                    jdbc.sql("""
                                    insert into variants (product_id, planter_id, size, height, sku, price,
                                                          compare_at_price, stock, dhaka_only)
                                    values (:p, :pl, :size, :height, :sku, :price, :compare, :stock, :dhaka)""")
                            .param("p", id).param("pl", planterId).param("size", size.size())
                            .param("height", size.height())
                            .param("sku", String.format(Locale.ROOT, "SB-%03d-P%d-%s", index + 1, pi + 1, size.size().charAt(0)))
                            .param("price", price).param("compare", compareAt)
                            .param("stock", stock(index, pi, si))
                            .param("dhaka", outdoorTree && size.size().equals("LARGE"))
                            .update();
                }
            }
        }

        /** Mostly healthy stock, with some options low and some sold out. */
        private static int stock(int product, int planter, int size) {
            int h = Math.abs((product * 31 + planter * 17 + size * 7) % 19);
            if (h == 0 || h == 11) {
                return 0;
            }
            return h < 4 ? h : 4 + h;
        }

        private static int round10(int amount) {
            return Math.round(amount / 10f) * 10;
        }

        private void image(long productId, long planterId, String url, String alt, int position) {
            jdbc.sql("""
                            insert into product_images (product_id, planter_id, url, alt, position)
                            values (:p, :pl, :url, :alt, :position)""")
                    .param("p", productId).param("pl", planterId).param("url", url).param("alt", alt)
                    .param("position", position).update();
        }

        private String write(String name, String svg) {
            try {
                Files.createDirectories(folder);
                Files.writeString(folder.resolve(name), svg);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return "/media/demo/" + name;
        }

        static String slug(String text) {
            String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
    }
}
