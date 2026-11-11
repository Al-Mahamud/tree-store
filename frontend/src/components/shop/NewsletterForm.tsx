"use client";

import { useState } from "react";
import { ArrowRight, Check } from "lucide-react";

/** Phase 7 stores subscribers; until then the form only confirms locally. */
export function NewsletterForm() {
  const [done, setDone] = useState(false);
  if (done) {
    return <p className="mt-6 flex items-center gap-2 text-sm text-lime"><Check size={16} /> You&rsquo;re on the list.</p>;
  }
  return (
    <form className="mt-6 flex max-w-md items-center gap-2 rounded-full bg-bone/10 p-1.5 focus-within:bg-bone/15" onSubmit={(e) => { e.preventDefault(); setDone(true); }}>
      <label htmlFor="newsletter" className="sr-only">Email address</label>
      <input id="newsletter" type="email" required placeholder="Your email" className="min-w-0 flex-1 bg-transparent px-4 text-sm outline-none placeholder:text-bone/45" />
      <button type="submit" className="inline-flex items-center gap-1.5 rounded-full bg-lime px-5 py-2.5 text-sm font-semibold text-forest-deep">
        Join <ArrowRight size={15} />
      </button>
    </form>
  );
}
