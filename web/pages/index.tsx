import React, { useEffect, useState } from "react";
import { fetchPatients, type PatientSummary } from "../lib/api";
import Link from "next/link";

export default function HomePage() {
  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await fetchPatients();
        if (!cancelled) {
          setPatients(data);
        }
      } catch (err: any) {
        if (!cancelled) {
          setError(err.message ?? "Failed to load patients");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main style={{ maxWidth: 800, margin: "2rem auto", padding: "0 1rem" }}>
      <h1>Hospital-in-a-Box</h1>
      <p>HL7 v2 → FHIR bridge and clinical event viewer</p>
      <p>
        <Link href="/hl7-sim">Open HL7 simulator</Link>
      </p>
      {loading && <p>Loading patients...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {!loading && !error && (
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            marginTop: "1rem",
          }}
        >
          <thead>
            <tr>
              <th
                style={{ textAlign: "left", borderBottom: "1px solid #e5e7eb" }}
              >
                MRN
              </th>
              <th
                style={{ textAlign: "left", borderBottom: "1px solid #e5e7eb" }}
              >
                Name
              </th>
              <th
                style={{ textAlign: "left", borderBottom: "1px solid #e5e7eb" }}
              >
                Birth Date
              </th>
              <th />
            </tr>
          </thead>
          <tbody>
            {patients.map((p) => (
              <tr key={p.id}>
                <td>{p.mrn}</td>
                <td>
                  {p.firstName ?? ""} {p.lastName ?? ""}
                </td>
                <td>{p.birthDate ?? ""}</td>
                <td>
                  <Link href={`/patients/${p.id}`}>View timeline</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
