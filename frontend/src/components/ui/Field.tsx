/** A labelled control with a hint or an error underneath. Wraps one input, select or textarea. */
export function Field({ label, hint, error, className = "", children }: { label: string; hint?: string; error?: string; className?: string; children: React.ReactNode }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-semibold text-ink-soft">{label}</span>
      <span className={`block rounded-2xl border bg-white transition focus-within:border-forest focus-within:ring-4 focus-within:ring-lime/40 [&>*]:w-full [&>*]:rounded-2xl [&>*]:bg-transparent [&>*]:px-4 [&>*]:py-3 [&>*]:text-sm [&>*]:outline-none [&>textarea]:resize-none ${error ? "border-sale" : "border-line"}`}>
        {children}
      </span>
      {error ? <span className="mt-1.5 block text-xs text-sale">{error}</span> : hint ? <span className="mt-1.5 block text-xs text-muted">{hint}</span> : null}
    </label>
  );
}
