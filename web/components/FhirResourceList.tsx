import React from "react";
import type { FhirResourceSummary } from "../lib/api";

type Props = {
  resources: FhirResourceSummary[];
};

export const FhirResourceList: React.FC<Props> = ({ resources }) => {
  if (!resources.length) {
    return <p>No FHIR resources yet.</p>;
  }

  return (
    <div className="fhir-list">
      {resources.map((r) => {
        let parsed: unknown = r.body;
        try {
          parsed = JSON.parse(r.body);
        } catch {
          // leave as string if not valid JSON
        }

        return (
          <section key={r.id} className="fhir-card">
            <div className="fhir-header">
              <span className="fhir-type">{r.resourceType}</span>
              <span className="fhir-id">id: {r.resourceId}</span>
              {r.eventTime && (
                <span className="fhir-time">
                  {new Date(r.eventTime).toLocaleString()}
                </span>
              )}
            </div>
            <pre className="fhir-body">
              {typeof parsed === "string"
                ? parsed
                : JSON.stringify(parsed, null, 2)}
            </pre>
          </section>
        );
      })}

      <style jsx>{`
        .fhir-list {
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }
        .fhir-card {
          border: 1px solid #e5e7eb;
          border-radius: 4px;
          padding: 0.75rem;
          background: #f9fafb;
        }
        .fhir-header {
          display: flex;
          gap: 0.75rem;
          align-items: baseline;
          margin-bottom: 0.5rem;
          flex-wrap: wrap;
        }
        .fhir-type {
          font-weight: 600;
        }
        .fhir-id,
        .fhir-time {
          font-size: 0.8rem;
          color: #6b7280;
        }
        .fhir-body {
          margin: 0;
          font-size: 0.8rem;
          max-height: 300px;
          overflow: auto;
          background: #111827;
          color: #e5e7eb;
          padding: 0.5rem;
          border-radius: 4px;
        }
      `}</style>
    </div>
  );
};
