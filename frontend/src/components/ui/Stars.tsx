import { Star } from "lucide-react";

/** Five stars filled to `value` (halves and tenths show as part-filled stars). */
export function Stars({ value, size = 14, className = "" }: { value: number; size?: number; className?: string }) {
  return (
    <span className={`relative inline-flex shrink-0 ${className}`} role="img" aria-label={`${value.toFixed(1)} out of 5 stars`}>
      <span className="flex gap-px text-line">
        {[0, 1, 2, 3, 4].map((i) => <Star key={i} size={size} fill="currentColor" strokeWidth={0} />)}
      </span>
      <span className="absolute inset-y-0 left-0 flex gap-px overflow-hidden text-sun" style={{ width: `${(Math.max(0, Math.min(5, value)) / 5) * 100}%` }}>
        {[0, 1, 2, 3, 4].map((i) => <Star key={i} size={size} fill="currentColor" strokeWidth={0} className="shrink-0" />)}
      </span>
    </span>
  );
}

/** "★ 4.8 (12)" for cards; nothing when a plant has no reviews yet. */
export function RatingBadge({ rating, count }: { rating: number | null; count: number }) {
  if (!rating || count === 0) return null;
  return (
    <span className="inline-flex items-center gap-1 text-xs text-muted" aria-label={`Rated ${rating.toFixed(1)} out of 5 from ${count} ${count === 1 ? "review" : "reviews"}`}>
      <Star size={13} className="text-sun" fill="currentColor" strokeWidth={0} aria-hidden />
      <span className="font-semibold text-ink" aria-hidden>{rating.toFixed(1)}</span>
      <span aria-hidden>({count})</span>
    </span>
  );
}
