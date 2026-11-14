import { site } from "./site";

/**
 * The information pages linked from the footer. Written for a shop that is not open yet: the owner
 * must check every promise here (and have Terms and Privacy read by a lawyer) before launch.
 * Delivery fees on the delivery page come live from the shop's settings.
 */
export type InfoPage = {
  slug: string;
  title: string;
  /** For search results and link previews. */
  description: string;
  intro: string;
  sections: { heading: string; body: string[] }[];
};

const days = site.guaranteeDays;

export const pages: InfoPage[] = [
  {
    slug: "about",
    title: "Our story",
    description: `${site.name} is a nursery near Dhaka that grows fruit trees, flowering trees and indoor plants and delivers them across Bangladesh.`,
    intro: `${site.name} (${site.localName}, "green") started as a family nursery in Savar. We wanted buying a tree to be as easy as ordering a book, without the guesswork about what will actually grow.`,
    sections: [
      {
        heading: "Grown, not just resold",
        body: [
          "Most of our plants are grown or grafted at our own nursery; the rest come from growers we have worked with for years. Every plant is hardened in real sun and rain before it is sold, so it is ready for a roof or a garden rather than a showroom.",
          "Fruit trees are grafted or air-layered from heavy-bearing mother trees, so they fruit years sooner than seedlings and stay true to their variety.",
        ],
      },
      {
        heading: "Honest labels",
        body: [
          "Every plant shows the light, water and care it needs, how tall it grows, whether it is safe around pets and, for fruit trees, roughly when it starts to fruit. If a plant isn't right for your space, we would rather tell you before you buy.",
        ],
      },
      {
        heading: "After it arrives",
        body: [
          `We text care tips in the first weeks, our planting guides are free to read, and every plant has a ${days}-day healthy-plant guarantee.`,
        ],
      },
    ],
  },
  {
    slug: "delivery",
    title: "Delivery areas",
    description: "How Shobuj delivers live plants inside and outside Dhaka, how long it takes, and what it costs.",
    intro: "We deliver everywhere in Bangladesh. Plants leave the nursery watered and packed upright so they arrive ready to plant.",
    sections: [
      {
        heading: "Inside Dhaka",
        body: [
          "Our own delivery team brings orders inside Dhaka city in 1–2 days after we confirm them by phone. Large trees (5–6 ft) travel only inside Dhaka, in our van, because couriers can't carry them safely.",
        ],
      },
      {
        heading: "Outside Dhaka",
        body: [
          "Small and medium plants go by courier (such as Sundarban Courier or SA Paribahan) and usually arrive in 2–4 days. We text you the courier's name and tracking number when the parcel leaves us. Some couriers deliver to their branch office, and they will call you to collect.",
        ],
      },
      {
        heading: "Fees",
        body: [
          "The delivery fee is shown in your bag and at checkout, and it is free above a certain order value (shown on every page). Cash on delivery is available everywhere; you can also pay online with bKash, Nagad or a card.",
        ],
      },
      {
        heading: "When it arrives",
        body: [
          "Open the box upright, water the plant if the soil feels dry, and keep it in light shade for a few days before planting or moving it into full sun. If anything is damaged, take a photo before you throw the packaging away and send it to us.",
        ],
      },
    ],
  },
  {
    slug: "guarantee",
    title: "Healthy-plant guarantee",
    description: `Every Shobuj plant comes with a ${days}-day healthy-plant guarantee: if it arrives damaged or fails to settle, we replace it.`,
    intro: `Live plants travel and settle in, and sometimes one doesn't make it. For ${days} days after delivery, if your plant arrives damaged or fails to settle despite reasonable care, we will replace it or refund it.`,
    sections: [
      {
        heading: "How to claim",
        body: [
          `Send us your order number and a photo or two of the plant on WhatsApp (${site.whatsapp}) within ${days} days of delivery. Our plant doctor may suggest a fix first; if the plant can't be saved, we send a replacement with your next delivery or refund the plant's price.`,
        ],
      },
      {
        heading: "What is covered",
        body: [
          "Plants that arrive broken or badly damaged, and plants that wilt, drop all their leaves or die within the guarantee period when they have been kept as the care label describes.",
        ],
      },
      {
        heading: "What isn't",
        body: [
          "A few yellow or dropped leaves after the journey (this is normal), damage from pets, pests or storms after delivery, plants moved far from the light on their label, and plants that were never watered. Planters are covered for breakage on arrival only.",
        ],
      },
    ],
  },
  {
    slug: "contact",
    title: "Contact us",
    description: "Call, WhatsApp or email Shobuj for help with an order, a plant or a bulk enquiry.",
    intro: "We are real gardeners and happy to help, whether it's about an order or a plant that looks unhappy.",
    sections: [
      { heading: "Phone", body: [`${site.phone}, 9 am to 8 pm every day.`] },
      { heading: "Plant doctor on WhatsApp", body: [`${site.whatsapp}. Send a photo of your plant and its surroundings and we'll tell you what it needs.`] },
      { heading: "Email", body: [site.email] },
      { heading: "Nursery", body: [`${site.address}. Visits by appointment: call ahead so a gardener can show you around.`] },
    ],
  },
  {
    slug: "bulk",
    title: "Corporate & bulk orders",
    description: "Trees and plants in bulk for offices, schools, housing projects, tree-planting drives and corporate gifts.",
    intro: "We supply offices, schools, housing societies and tree-planting drives, from fifty saplings to a few thousand.",
    sections: [
      {
        heading: "What we can do",
        body: [
          "Saplings for monsoon planting drives, fruit trees for housing projects and rooftops, indoor plants in matching planters for offices, and gift plants with a card for Eid, events and clients.",
          "For larger orders we can deliver and plant on site inside Dhaka, and set up regular care visits for office plants.",
        ],
      },
      {
        heading: "How to ask",
        body: [`Email ${site.email} or call ${site.phone} with the plants you have in mind, how many, where they are going and when. We reply with a quote within two working days.`],
      },
    ],
  },
  {
    slug: "privacy",
    title: "Privacy",
    description: "What Shobuj collects about you, why, and what you can ask us to do with it.",
    intro: "We collect only what we need to deliver your plants and run the shop, and we never sell it.",
    sections: [
      {
        heading: "What we keep",
        body: [
          "Your name, mobile number, delivery address, optional email and what you ordered. If you make an account, we also keep a scrambled form of your password (we can't read it), your saved addresses, saved plants and reviews.",
          "When you pay online, SSLCommerz handles your card or mobile wallet details; we only receive whether the payment succeeded and its reference.",
        ],
      },
      {
        heading: "What we use it for",
        body: [
          "To deliver and track orders, to text you about your order and, unless you turn them off, care tips for your plants after delivery. We share your name, phone and address with the courier carrying your parcel and nobody else, except where the law requires it.",
        ],
      },
      {
        heading: "Reviews",
        body: ["A review shows your first name and the first letter of your last name, the size and planter you bought, and the date. You can change or delete your review from your account at any time."],
      },
      {
        heading: "Your choices",
        body: [`You can update your details in your account, turn off care texts on any order page, and ask us to delete your account by writing to ${site.email}. We keep order records for as long as tax law requires.`],
      },
    ],
  },
  {
    slug: "terms",
    title: "Terms of sale",
    description: "The terms that apply when you buy plants from Shobuj.",
    intro: "These terms apply to orders placed on this website. By placing an order you agree to them.",
    sections: [
      {
        heading: "Orders and prices",
        body: [
          "Prices are in taka and include VAT where it applies. We confirm every order by phone before sending it. We may cancel an order (and refund anything paid) if a plant is no longer available or a price was shown wrongly.",
        ],
      },
      {
        heading: "Live plants",
        body: [
          "Plants are living things and vary: each one is a little different from the photo in shape, height (within the range shown) and number of leaves. Fruiting times are typical for good care and are not a promise.",
        ],
      },
      {
        heading: "Cancelling and returns",
        body: [
          `You can cancel free of charge until we send your order out; call us. Because plants are perishable, they can't be returned once delivered, but the ${days}-day healthy-plant guarantee covers plants that arrive damaged or fail to settle. Online payments for cancelled orders are refunded to the same account within 7 working days.`,
        ],
      },
      {
        heading: "Reviews",
        body: ["Only customers whose plant was delivered can review it. We may hide reviews that are abusive, contain personal details or aren't about the plant."],
      },
    ],
  },
];

export const pageSlugs = pages.map((p) => p.slug);
