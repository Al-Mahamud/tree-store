/** Dates and times as the shop reads them, in Dhaka time. */
const dateTimeFormat = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit", timeZone: "Asia/Dhaka" });
const dayFormat = new Intl.DateTimeFormat("en-GB", { weekday: "short", day: "numeric", month: "short", timeZone: "Asia/Dhaka" });

export function dateTime(iso: string): string {
  return dateTimeFormat.format(new Date(iso));
}

/** A calendar date ("2026-11-14") as "Sat 14 Nov". */
export function day(date: string): string {
  return dayFormat.format(new Date(`${date}T12:00:00+06:00`));
}

/** "3 h ago", "2 d ago": how long an order has waited. */
export function ago(iso: string): string {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  return hours < 48 ? `${hours} h ago` : `${Math.round(hours / 24)} d ago`;
}
