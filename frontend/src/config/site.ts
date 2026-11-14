/** Brand settings in one place: change the name, contact details and promises here. */
export const site = {
  name: "Shobuj",
  localName: "সবুজ",
  tagline: "Trees and plants, delivered ready to grow",
  description:
    "Grafted fruit trees, flowering trees, palms, indoor plants and bonsai from our nursery, delivered across Bangladesh with a 30-day healthy-plant guarantee.",
  phone: "+880 1700-000000",
  whatsapp: "+880 1700-000000",
  email: "hello@shobuj.example",
  address: "Nursery Road, Savar, Dhaka",
  /** Only a fallback: the shop sets delivery fees in the admin area. */
  freeDeliveryOver: 5000,
  deliveryFees: { insideDhaka: 100, outsideDhaka: 200 },
  guaranteeDays: 30,
} as const;
