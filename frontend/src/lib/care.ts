/** Words for the care facts the API returns as codes. */
export const lightLabels = {
  LOW: "Low light",
  MEDIUM: "Medium light",
  BRIGHT: "Bright indirect",
  FULL_SUN: "Full sun",
} as const;

export const waterLabels = {
  LOW: "Water rarely",
  MEDIUM: "Water weekly",
  HIGH: "Keep moist",
} as const;

export const careLabels = {
  EASY: "Easy care",
  MODERATE: "Some care",
  EXPERT: "For experienced growers",
} as const;

export const placementLabels = {
  INDOOR: "Indoor",
  OUTDOOR: "Outdoor",
  BOTH: "Indoor or outdoor",
} as const;

export const growthLabels = { SLOW: "Slow", MEDIUM: "Steady", FAST: "Fast" } as const;

export type Light = keyof typeof lightLabels;
export type Water = keyof typeof waterLabels;
export type CareLevel = keyof typeof careLabels;
export type Placement = keyof typeof placementLabels;
