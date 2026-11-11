import { Droplet, Droplets, Leaf, Sprout, Sun, SunDim, SunMedium, CloudSun } from "lucide-react";
import type { CareLevel, Light, Water } from "@/lib/care";

export function LightIcon({ light, size = 16 }: { light: Light; size?: number }) {
  const Icon = { LOW: CloudSun, MEDIUM: SunDim, BRIGHT: SunMedium, FULL_SUN: Sun }[light];
  return <Icon size={size} strokeWidth={1.7} aria-hidden />;
}

export function WaterIcon({ water, size = 16 }: { water: Water; size?: number }) {
  const Icon = water === "HIGH" ? Droplets : Droplet;
  return <Icon size={size} strokeWidth={1.7} aria-hidden />;
}

export function CareLevelIcon({ level, size = 16 }: { level: CareLevel; size?: number }) {
  const Icon = level === "EASY" ? Sprout : Leaf;
  return <Icon size={size} strokeWidth={1.7} aria-hidden />;
}
