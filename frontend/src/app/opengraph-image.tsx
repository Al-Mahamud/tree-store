import { ImageResponse } from "next/og";
import { site } from "@/config/site";

export const alt = `${site.name} — ${site.tagline}`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/** The heading font as TrueType (what the image renderer reads); null falls back to the default font. */
async function headingFont(): Promise<ArrayBuffer | null> {
  try {
    const css = await (await fetch("https://fonts.googleapis.com/css2?family=Instrument+Serif")).text();
    const url = css.match(/src: url\((.+?)\) format\('(?:truetype|opentype)'\)/)?.[1];
    return url ? await (await fetch(url)).arrayBuffer() : null;
  } catch {
    return null;
  }
}

/** The picture shown when someone shares a Shobuj link (plant pages use the plant's own picture). */
export default async function OpenGraphImage() {
  const font = await headingFont();
  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between", background: "#142a1f", color: "#f4f2ec", padding: 72, fontFamily: font ? "Instrument" : undefined }}>
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <svg width="84" height="84" viewBox="0 0 32 32">
            <circle cx="16" cy="16" r="16" fill="#d6ee7a" />
            <path d="M16 25V13" stroke="#142a1f" strokeWidth="1.8" strokeLinecap="round" />
            <path d="M16 15c0-4 3-7 7-7 0 4-3 7-7 7Z" fill="#142a1f" />
            <path d="M16 19c0-3-2.5-5.5-6-5.5 0 3 2.5 5.5 6 5.5Z" fill="#142a1f" opacity=".8" />
          </svg>
          <div style={{ display: "flex", fontSize: 72 }}>{site.name}</div>
        </div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ display: "flex", flexDirection: "column", fontSize: 96, lineHeight: 0.98, maxWidth: 980 }}>
            <span>Plant a tree.</span>
            <span style={{ color: "#d6ee7a", fontStyle: "italic" }}>Grow a legacy.</span>
          </div>
          <div style={{ marginTop: 32, fontSize: 30, opacity: 0.8, fontFamily: "sans-serif" }}>
            {`Fruit trees, flowering trees & indoor plants · Delivered across Bangladesh · ${site.guaranteeDays}-day guarantee`}
          </div>
        </div>
      </div>
    ),
    { ...size, fonts: font ? [{ name: "Instrument", data: font, weight: 400, style: "normal" }] : undefined },
  );
}
