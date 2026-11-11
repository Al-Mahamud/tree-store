"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Search, X } from "lucide-react";

const suggestions = ["Mango", "Lemon", "Snake plant", "Bonsai", "আম", "Krishnachura", "Pet-safe"];

export function SearchButton() {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const input = useRef<HTMLInputElement>(null);
  const router = useRouter();

  useEffect(() => {
    if (!open) return;
    input.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  const go = (text: string) => {
    if (!text.trim()) return;
    setOpen(false);
    router.push(text === "Pet-safe" ? "/shop?petFriendly=true" : `/search?q=${encodeURIComponent(text.trim())}`);
  };

  return (
    <>
      <button type="button" aria-label="Search" onClick={() => setOpen(true)} className="grid h-10 w-10 place-items-center rounded-full hover:bg-mist">
        <Search size={20} strokeWidth={1.7} />
      </button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[12vh]" role="dialog" aria-modal aria-label="Search">
          <div className="absolute inset-0 bg-forest-deep/40 backdrop-blur-sm" onClick={() => setOpen(false)} />
          <div className="relative w-full max-w-2xl animate-rise rounded-[2rem] bg-bone p-3 shadow-2xl">
            <form
              className="flex items-center gap-3 rounded-full bg-white px-5 py-3"
              onSubmit={(e) => {
                e.preventDefault();
                go(q);
              }}
            >
              <Search size={20} className="shrink-0 text-muted" />
              <input
                ref={input}
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search by name, Bangla name or botanical name"
                aria-label="Search plants"
                className="min-w-0 flex-1 bg-transparent text-base outline-none placeholder:text-muted/70"
              />
              <button type="submit" aria-label="Search" className="grid h-10 w-10 place-items-center rounded-full bg-forest text-bone">
                <ArrowRight size={18} />
              </button>
            </form>
            <div className="flex flex-wrap items-center gap-2 px-3 pb-3 pt-4">
              <span className="kicker mr-1 text-muted">Try</span>
              {suggestions.map((s) => (
                <button key={s} type="button" onClick={() => go(s)} className="rounded-full border border-line px-3.5 py-1.5 text-sm hover:border-forest hover:bg-forest hover:text-bone">
                  {s}
                </button>
              ))}
            </div>
            <button type="button" aria-label="Close search" onClick={() => setOpen(false)} className="absolute -right-2 -top-2 grid h-9 w-9 place-items-center rounded-full bg-forest-deep text-bone">
              <X size={16} />
            </button>
          </div>
        </div>
      )}
    </>
  );
}
