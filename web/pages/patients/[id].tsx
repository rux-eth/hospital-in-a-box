import React, { useEffect, useState } from "react";
import { useRouter } from "next/router";
import Link from "next/link";
import {
  fetchTimeline,
  fetchFhirResources,
  type TimelineEvent,
  type FhirResourceSummary,
} from "../../lib/api";
import { Timeline } from "../../components/Timeline";
import { FhirResourceList } from "../../components/FhirResourceList";

type Tab = "timeline" | "fhir";

export default function PatientTimelinePage() {
  const router = useRouter();
  const { id } = router.query;

  const [activeTab, setActiveTab] = useState<Tab>("timeline");

  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [eventsError, setEventsError] = useState<string | null>(null);

  const [fhirResources, setFhirResources] = useState<FhirResourceSummary[]>([]);
  const [fhirLoading, setFhirLoading] = useState(true);
  const [fhirError, setFhirError] = useState<string | null>(null);

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
          setEventsError(err.message ?? "Failed to load timeline");
        }
      } finally {
        if (!cancelled) setEventsLoading(false);
      }
    })();

    (async () => {
      try {
        const data = await fetchFhirResources(id);
        if (!cancelled) {
          setFhirResources(data);
        }
      } catch (err: any) {
        if (!cancelled) {
          setFhirError(err.message ?? "Failed to load FHIR resources");
        }
      } finally {
        if (!cancelled) setFhirLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <main style={{ maxWidth: 900, margin: "2rem auto", padding: "0 1rem" }}>
      <p>
        <Link href="/">&larr; Back to patients</Link>
      </p>
      <h1>Patient Detail</h1>

      {/* Tabs */}
      <div style={{ marginTop: "1rem", marginBottom: "1rem" }}>
        <button
          className={activeTab === "timeline" ? "tab tab-active" : "tab"}
          onClick={() => setActiveTab("timeline")}
        >
          Clinical timeline
        </button>
        <button
          className={activeTab === "fhir" ? "tab tab-active" : "tab"}
          onClick={() => setActiveTab("fhir")}
        >
          Raw FHIR resources
        </button>
        <style jsx>{`
          .tab {
            padding: 0.35rem 0.75rem;
            border-radius: 4px 4px 0 0;
            border: 1px solid #d1d5db;
            background-color: #f9fafb;
            margin-right: 0.25rem;
            cursor: pointer;
            font-size: 0.9rem;
          }
          .tab-active {
            border-bottom-color: #ffffff;
            background-color: #ffffff;
            font-weight: 600;
          }
        `}</style>
      </div>

      {/* Content */}
      <div
        style={{
          border: "1px solid #d1d5db",
          borderRadius: "0 4px 4px 4px",
          padding: "1rem",
          background: "#ffffff",
        }}
      >
        {activeTab === "timeline" ? (
          <>
            {eventsLoading && <p>Loading timeline...</p>}
            {eventsError && <p style={{ color: "red" }}>{eventsError}</p>}
            {!eventsLoading && !eventsError && <Timeline events={events} />}
          </>
        ) : (
          <>
            {fhirLoading && <p>Loading FHIR resources...</p>}
            {fhirError && <p style={{ color: "red" }}>{fhirError}</p>}
            {!fhirLoading && !fhirError && (
              <FhirResourceList resources={fhirResources} />
            )}
          </>
        )}
      </div>
    </main>
  );
}
