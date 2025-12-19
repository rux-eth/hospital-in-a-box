const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export type PatientSummary = {
  id: string;
  mrn: string;
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  gender: string | null;
};

export type TimelineEvent = {
  timestamp: string;
  type: "ADMIT" | "OBSERVATION" | "DISCHARGE" | string;
  title: string;
  description: string;
  encounterId: string | null;
  observationId: string | null;
};

export async function fetchPatients(): Promise<PatientSummary[]> {
  const res = await fetch(`${API_BASE_URL}/api/patients`);
  if (!res.ok) {
    throw new Error(`Failed to fetch patients: ${res.status}`);
  }
  return res.json();
}

export async function fetchTimeline(
  patientId: string
): Promise<TimelineEvent[]> {
  const res = await fetch(`${API_BASE_URL}/api/patients/${patientId}/timeline`);
  if (!res.ok) {
    throw new Error(`Failed to fetch timeline: ${res.status}`);
  }
  return res.json();
}

export async function postHl7Message(raw: string): Promise<unknown> {
  const res = await fetch(`${API_BASE_URL}/api/hl7/messages`, {
    method: "POST",
    headers: {
      "Content-Type": "text/plain",
    },
    body: raw,
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HL7 POST failed: ${res.status} ${text}`);
  }
  return res.json();
}
