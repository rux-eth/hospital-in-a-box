import React, { useEffect, useState } from "react";
import { useRouter } from "next/router";
import Link from "next/link";
import { fetchTimeline, type TimelineEvent } from "../../lib/api";
import { Timeline } from "../../components/Timeline";

export default function PatientTimelinePage() {
  const router = useRouter();
  const { id } = router.query;

  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id || typeof id !== "string") return;

    let cancelled = false;
    (async () => {
      try {
        const data = await fetchTimeline(id);
        if (!cancelled) {
          setEvents(data);
        }
      } catch (err: any) {
        if (!cancelled) {
          setError(err.message ?? "Failed to load timeline");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <main style={{ maxWidth: 800, margin: "2rem auto", padding: "0 1rem" }}>
      <p>
        <Link href="/">&larr; Back to patients</Link>
      </p>
      <h1>Patient Timeline</h1>

      {loading && <p>Loading timeline...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {!loading && !error && <Timeline events={events} />}
    </main>
  );
}
