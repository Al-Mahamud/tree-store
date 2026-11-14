/**
 * Planting and care guides, written for Bangladesh's climate. Product pages link to the guide whose
 * categories match the plant; care texts link to /guides. Plain advice the nursery should check
 * against its own practice before launch.
 */
export type GuideIcon = "tree" | "sun" | "leaf" | "droplet" | "cloud" | "bonsai";

export type Guide = {
  slug: string;
  title: string;
  /** One line for cards and search results. */
  summary: string;
  icon: GuideIcon;
  /** Card colour: a theme colour name. */
  tone: "lime" | "sage" | "clay" | "mist";
  minutes: number;
  /** Category slugs whose plants link to this guide. */
  categories: string[];
  /** The shop listing (query string) shown under the guide as "plants for this guide". */
  plants: string;
  /** A short checklist at the top. */
  youNeed?: string[];
  sections: { heading: string; body: string[]; tips?: string[] }[];
};

export const guides: Guide[] = [
  {
    slug: "planting-a-fruit-tree",
    title: "Planting a grafted fruit tree",
    summary: "From grow bag to ground or drum: the hole, the soil mix, the graft line and the first month of watering.",
    icon: "tree",
    tone: "lime",
    minutes: 6,
    categories: ["fruit-trees"],
    plants: "category=fruit-trees",
    youNeed: ["A spot with 6+ hours of sun", "Well-rotted cow dung or compost", "A handful of bone meal (optional)", "A bamboo stake and soft string"],
    sections: [
      {
        heading: "Let it rest first",
        body: [
          "A tree that has just travelled is thirsty and a little stressed. Water it in its bag and keep it in light shade for three or four days before planting. Plant on a cool evening or a cloudy day, never in midday heat.",
        ],
      },
      {
        heading: "Dig a generous hole",
        body: [
          "For a tree in the ground, dig a hole about 60 cm wide and 60 cm deep (two feet each way). Keep the top soil to one side. Mix it roughly two parts soil to one part rotted cow dung or compost, plus a handful of bone meal if you have it.",
          "On a rooftop, use a half drum or a pot at least 50 cm across with drainage holes. Fill it with one part garden soil, one part compost and one part coarse sand or brick chips so water drains quickly.",
        ],
      },
      {
        heading: "Mind the graft line",
        body: [
          "Grafted trees have a visible joint a hand's width above the soil, where the fruiting variety meets the root stock. Keep that joint above the soil when you plant: buried, it can rot or send up wild shoots.",
          "Cut the grow bag away rather than pulling the tree out, so the root ball stays whole. Set the tree at the same depth it grew in the bag, fill around it, and press the soil down firmly with your hands.",
        ],
        tips: ["Rub off any shoots that appear below the graft line.", "Tie the trunk loosely to a stake for the first monsoon."],
      },
      {
        heading: "Water deeply, then less often",
        body: [
          "Water slowly until the whole root ball is soaked. For the first two weeks, water every day or two in dry weather; after that, water when the top 3 cm of soil is dry. Trees in drums dry out faster than trees in the ground, especially in April and May.",
          "A mulch of dry leaves or straw around the base (not touching the trunk) keeps the soil cool and moist.",
        ],
      },
      {
        heading: "The first year",
        body: [
          "Many grafted trees flower in their first year. Pick off the first flowers or fruit so the tree puts its strength into roots and branches; let it fruit from the second season.",
          "Feed with a few handfuls of compost every two to three months during the growing season, and less in winter.",
        ],
      },
    ],
  },
  {
    slug: "rooftop-fruit-garden",
    title: "A fruit garden on the roof",
    summary: "Which trees suit drums, how to protect the roof slab, and how to keep pots cool through a Dhaka summer.",
    icon: "sun",
    tone: "clay",
    minutes: 5,
    categories: [],
    plants: "collection=rooftop-garden",
    sections: [
      {
        heading: "Choose compact trees",
        body: [
          "Dwarf and grafted varieties fruit well in containers: Amrapali mango, kagzi lemon, malta, guava, pomegranate and dwarf coconut all do. Jackfruit, lychee and timber trees need open ground.",
        ],
      },
      {
        heading: "Protect the roof",
        body: [
          "Stand drums on bricks or a metal stand so water can drain and the slab can dry. Keep heavy containers close to walls and columns, where the roof is strongest, and ask your building's engineer before putting many large drums in one place.",
        ],
      },
      {
        heading: "Beat the heat",
        body: [
          "Roof slabs get very hot in April and May. Clay pots stay cooler than plastic; a layer of mulch on top and a shade net for the hottest weeks help a lot. Water in the early morning or evening, never at noon.",
        ],
        tips: ["Group pots together: they shade each other and keep moisture longer.", "Check drainage holes after every monsoon storm."],
      },
      {
        heading: "Feed little and often",
        body: [
          "Container soil runs out of food quicker than the ground. Top up with compost every two months and refresh the top 5 cm of soil every year before the monsoon.",
        ],
      },
    ],
  },
  {
    slug: "indoor-plants-first-month",
    title: "Your indoor plant's first month",
    summary: "Where to put it, when to water, why a leaf might yellow, and when it's safe to repot.",
    icon: "leaf",
    tone: "sage",
    minutes: 4,
    categories: ["indoor", "indoor-easy", "indoor-statement", "indoor-trailing"],
    plants: "category=indoor",
    sections: [
      {
        heading: "Find its spot and leave it there",
        body: [
          "Plants dislike being moved around. Pick a place that matches its light on the label and leave it for a few weeks. 'Bright indirect' means near a window but out of direct afternoon sun; 'low light' plants manage a few metres from a window.",
          "Keep plants out of the direct blast of an air conditioner or a ceiling fan running all day: the dry air browns leaf edges.",
        ],
      },
      {
        heading: "Water by touch, not by calendar",
        body: [
          "Push a finger into the soil. If the top 2–3 cm are dry, water until it runs out of the bottom, then empty the saucer. Most indoor plants are killed by too much water, not too little.",
        ],
      },
      {
        heading: "A yellow leaf is normal",
        body: [
          "After a journey, one or two lower leaves may yellow and drop. That's the plant adjusting. Several yellow leaves at once usually means wet soil: water less and make sure the pot drains.",
        ],
      },
      {
        heading: "Repot later, not now",
        body: [
          "Our ceramic pots are sized for the plant and have drainage. Wait at least three weeks before repotting, and only move up one pot size (3–5 cm wider) when roots come out of the bottom.",
        ],
        tips: ["Wipe big leaves with a damp cloth every few weeks so they can breathe.", "Turn the pot a quarter every week so it grows evenly."],
      },
    ],
  },
  {
    slug: "watering-guide",
    title: "How much water does it need?",
    summary: "Reading the soil, watering through the seasons, and the signs of too much and too little.",
    icon: "droplet",
    tone: "mist",
    minutes: 4,
    categories: [],
    plants: "sort=popular",
    sections: [
      {
        heading: "Low, medium and high",
        body: [
          "Every plant page shows a water need. Low: let the soil dry out between waterings (snake plant, ZZ, pomegranate). Medium: water when the top 2–3 cm feel dry (most fruit trees and indoor plants). High: keep the soil evenly moist but never waterlogged (lychee, coconut, areca palm).",
        ],
      },
      {
        heading: "Through the year",
        body: [
          "In the hot months before the monsoon, pots on a roof may need water every day. During the monsoon, check before watering: rain often does the job. In winter, plants grow slowly and need about half as much.",
        ],
      },
      {
        heading: "Too much or too little?",
        body: [
          "Too much: yellow lower leaves, soft stems, soil that smells sour, tiny flies around the pot. Too little: dry, crispy leaf edges, drooping that recovers within hours of watering, soil pulling away from the pot.",
        ],
        tips: ["Water the soil, not the leaves.", "Morning is the best time; evening is next best."],
      },
    ],
  },
  {
    slug: "monsoon-tree-planting",
    title: "Planting trees in the monsoon",
    summary: "Why June to September is the best time to plant shade, timber, flowering trees and palms, and how to do it.",
    icon: "cloud",
    tone: "sage",
    minutes: 5,
    categories: ["trees", "shade-timber", "flowering-trees", "palms"],
    plants: "collection=monsoon-planting",
    sections: [
      {
        heading: "Why the monsoon",
        body: [
          "Steady rain and cloudy skies let young trees grow roots before the dry season. A tree planted in July has months of free water; the same tree planted in April needs watering every day.",
        ],
      },
      {
        heading: "Leave room to grow",
        body: [
          "Check the mature height on the plant page. Big trees like mahogany, neem, segun and krishnachura need at least 5–6 metres from buildings, walls and each other. Keep them away from septic tanks and pipes.",
        ],
      },
      {
        heading: "Plant above the water",
        body: [
          "In low land that floods, plant on a small raised mound so the roots don't sit in water. Dig the hole twice as wide as the root ball, mix in compost, and firm the soil well.",
        ],
        tips: ["Put a bamboo guard around young trees where goats or cattle graze.", "Water during dry spells in the first winter."],
      },
    ],
  },
  {
    slug: "bonsai-basics",
    title: "Bonsai basics",
    summary: "Sun, water, feeding and trimming a bonsai so it keeps its shape for years.",
    icon: "bonsai",
    tone: "clay",
    minutes: 5,
    categories: ["bonsai"],
    plants: "category=bonsai",
    sections: [
      {
        heading: "Most bonsai live outdoors",
        body: [
          "A bonsai is a real tree in a small tray. Ficus copes with a bright window, but flowering bonsai like bougainvillea need several hours of direct sun on a balcony or roof to stay healthy and flower.",
        ],
      },
      {
        heading: "Water carefully",
        body: [
          "Shallow trays dry out fast. Check every day: when the soil surface looks dry, water until it runs out of the drainage holes. In summer that can mean twice a day; never let the soil go bone dry.",
        ],
      },
      {
        heading: "Trim to keep the shape",
        body: [
          "Let new shoots grow 6–8 leaves, then cut them back to 2–3. Trim in the growing season, not in winter. Our trees come shaped; the wire on some branches can come off after a few months, before it bites into the bark.",
        ],
        tips: ["Feed with a little liquid fertiliser every two weeks in the growing season.", "Repot every two to three years, just before the monsoon."],
      },
    ],
  },
];

/** The guide for a plant, from its category path (most specific first). */
export function guideFor(categorySlugs: string[]): Guide | undefined {
  for (const slug of [...categorySlugs].reverse()) {
    const guide = guides.find((g) => g.categories.includes(slug));
    if (guide) return guide;
  }
  return undefined;
}
