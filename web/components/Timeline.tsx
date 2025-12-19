import React from "react";
import type { TimelineEvent } from "../lib/api";

type Props = {
  events: TimelineEvent[];
};

export const Timeline: React.FC<Props> = ({ events }) => {
  if (!events.length) {
    return <p>No clinical events yet.</p>;
  }

  return (
    <ol className="timeline">
      {events.map((e, idx) => (
        <li key={`${e.type}-${e.timestamp}-${idx}`} className="timeline-item">
          <div className="timeline-timestamp">
            {new Date(e.timestamp).toLocaleString()}
          </div>
          <div
            className={`timeline-type timeline-type-${e.type.toLowerCase()}`}
          >
            {e.type}
          </div>
          <div className="timeline-title">{e.title}</div>
          <div className="timeline-description">{e.description}</div>
        </li>
      ))}
      <style jsx>{`
        .timeline {
          list-style: none;
          padding: 0;
          margin: 0;
          border-left: 2px solid #e5e7eb;
        }
        .timeline-item {
          margin-left: 1rem;
          padding: 0.75rem 0 0.75rem 1rem;
          position: relative;
        }
        .timeline-item::before {
          content: "";
          position: absolute;
          left: -10px;
          top: 1.25rem;
          width: 10px;
          height: 10px;
          border-radius: 9999px;
          background-color: #3b82f6;
        }
        .timeline-timestamp {
          font-size: 0.75rem;
          color: #6b7280;
        }
        .timeline-type {
          display: inline-block;
          font-size: 0.75rem;
          padding: 0.1rem 0.4rem;
          border-radius: 9999px;
          background-color: #e5e7eb;
          margin-top: 0.25rem;
          margin-bottom: 0.25rem;
        }
        .timeline-type-admit {
          background-color: #d1fae5;
          color: #065f46;
        }
        .timeline-type-observation {
          background-color: #dbeafe;
          color: #1d4ed8;
        }
        .timeline-type-discharge {
          background-color: #fee2e2;
          color: #b91c1c;
        }
        .timeline-title {
          font-weight: 600;
        }
        .timeline-description {
          font-size: 0.9rem;
          color: #4b5563;
        }
      `}</style>
    </ol>
  );
};
