/** Browser-side API calls: same-origin, with the CSRF token Spring Security expects on writes. */

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

async function csrfToken(): Promise<string> {
  const existing = readCookie("XSRF-TOKEN");
  if (existing) return existing;
  await fetch("/api/v1/csrf", { credentials: "same-origin" });
  return readCookie("XSRF-TOKEN") ?? "";
}

export async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = await csrfToken();
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      headers,
      credentials: "same-origin",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "network", "We couldn't reach the shop. Check your connection and try again.");
  }
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.code ?? "unknown",
      data?.message ?? "Something went wrong. Please try again.",
      data?.fields ?? {},
    );
  }
  return data as T;
}
