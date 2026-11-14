"use client";

import { useRef, useState } from "react";
import { ImagePlus } from "lucide-react";
import { ApiError, upload } from "@/lib/client";
import { useAdmin } from "./AdminShell";

/** A picture with a button to replace it; uploads at once and hands back the API's answer. */
/* eslint-disable @next/next/no-img-element */
export function PictureButton<T>({ image, path, label, onDone, className = "h-14 w-14" }: { image: string | null; path: string; label: string; onDone: (result: T) => void; className?: string }) {
  const { notify } = useAdmin();
  const file = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  return (
    <>
      <button type="button" aria-label={label} title={label} disabled={busy} onClick={() => file.current?.click()} className={`group relative shrink-0 overflow-hidden rounded-2xl bg-mist ${className}`}>
        {image && <img src={image} alt="" className="h-full w-full object-cover" />}
        <span className={`absolute inset-0 grid place-items-center bg-forest-deep/50 text-bone transition ${image ? "opacity-0 group-hover:opacity-100 group-focus-visible:opacity-100" : "bg-transparent text-muted"} ${busy ? "opacity-100" : ""}`}><ImagePlus size={18} /></span>
      </button>
      <input
        ref={file}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        hidden
        onChange={async (e) => {
          const f = e.target.files?.[0];
          e.target.value = "";
          if (!f) return;
          setBusy(true);
          try {
            onDone(await upload<T>(path, f));
            notify("Picture saved");
          } catch (err) {
            notify(err instanceof ApiError ? err.message : "Upload failed.");
          } finally {
            setBusy(false);
          }
        }}
      />
    </>
  );
}
