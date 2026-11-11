"use client";

import { useRef, useState } from "react";

/* eslint-disable @next/next/no-img-element */
/** Main image with thumbnails on larger screens; a swipeable strip with dots on phones. */
export function Gallery({ images, name }: { images: { url: string; alt: string }[]; name: string }) {
  const [index, setIndex] = useState(0);
  const strip = useRef<HTMLDivElement>(null);
  if (images.length === 0) return <div className="aspect-[4/5] rounded-[2rem] bg-mist" aria-label={`No photo of ${name} yet`} />;
  const current = images[Math.min(index, images.length - 1)]!;
  return (
    <div>
      <div className="relative hidden overflow-hidden rounded-[2rem] bg-mist md:block">
        <img key={current.url} src={current.url} alt={current.alt || name} className="aspect-[4/5] w-full object-cover" />
      </div>
      {images.length > 1 && (
        <div className="mt-3 hidden gap-3 md:flex">
          {images.map((image, i) => (
            <button key={image.url} type="button" onClick={() => setIndex(i)} aria-label={`Show image ${i + 1}`} aria-pressed={i === index} className={`w-24 overflow-hidden rounded-2xl ring-2 ring-offset-2 ring-offset-bone transition ${i === index ? "ring-forest" : "ring-transparent opacity-70 hover:opacity-100"}`}>
              <img src={image.url} alt="" className="aspect-[4/5] w-full object-cover" />
            </button>
          ))}
        </div>
      )}
      <div ref={strip} onScroll={() => strip.current && setIndex(Math.round(strip.current.scrollLeft / strip.current.clientWidth))} className="no-scrollbar flex snap-x snap-mandatory gap-2 overflow-x-auto md:hidden">
        {images.map((image) => (
          <img key={image.url} src={image.url} alt={image.alt || name} className="aspect-[4/5] w-full shrink-0 snap-center rounded-[1.75rem] object-cover" />
        ))}
      </div>
      {images.length > 1 && (
        <div className="mt-3 flex justify-center gap-1.5 md:hidden" aria-hidden>
          {images.map((image, i) => <span key={image.url} className={`h-1.5 rounded-full transition-all ${i === index ? "w-6 bg-forest" : "w-1.5 bg-forest/25"}`} />)}
        </div>
      )}
    </div>
  );
}
