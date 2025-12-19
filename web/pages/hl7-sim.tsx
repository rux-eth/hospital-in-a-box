import React, { useState } from "react";
import Link from "next/link";
import { postHl7Message } from "../lib/api";

type Hl7FormState = {
  mrn: string;
  firstName: string;
  lastName: string;
  birthDate: string; // YYYY-MM-DD
  gender: "M" | "F" | "U";

  visitNumber: string;

  admitDateTime: string; // ISO local-like: 2025-01-01T12:00
  dischargeDateTime: string;

  obsCode: string;
  obsDisplay: string;
  obsValue: string;
  obsUnit: string;
  obsDateTime: string;
};

const initialState: Hl7FormState = {
  mrn: "12345",
  firstName: "JOHN",
  lastName: "DOE",
  birthDate: "1980-01-01",
  gender: "M",
  visitNumber: "VN12345",
  admitDateTime: "2025-01-01T12:00",
  dischargeDateTime: "2025-01-02T10:00",
  obsCode: "GLUCOSE",
  obsDisplay: "Glucose",
  obsValue: "105",
  obsUnit: "mg/dL",
  obsDateTime: "2025-01-01T12:59",
};

function toHl7Ts(local: string): string {
  // from "2025-01-01T12:00" -> "202501011200"
  if (!local) return "";
  const [date, time] = local.split("T");
  return (
    date.replace(/-/g, "") + (time ? time.replace(":", "").substring(0, 4) : "")
  );
}

function toHl7Date(date: string): string {
  // "1980-01-01" -> "19800101"
  return date ? date.replace(/-/g, "") : "";
}

function buildAdtA01(f: Hl7FormState): string {
  const msh7 = toHl7Ts(f.admitDateTime) || "202501011200";
  const pid7 = toHl7Date(f.birthDate);
  const pv1_44 = toHl7Ts(f.admitDateTime);

  return [
    `MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|${msh7}||ADT^A01|MSGID1234|P|2.5`,
    `PID|1||${f.mrn}^^^HOSPITAL^MR||${f.lastName}^${f.firstName}^^^^^L||${pid7}|${f.gender}`,
    `PV1|1|I|WARD^ROOM^BED|||||||||||||||${f.visitNumber}|||||||||||||||||||||||||${pv1_44}`,
  ].join("\n");
}

function buildAdtA03(f: Hl7FormState): string {
  const msh7 = toHl7Ts(f.dischargeDateTime) || "202501021000";
  const pid7 = toHl7Date(f.birthDate);
  const pv1_45 = toHl7Ts(f.dischargeDateTime);

  return [
    `MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|${msh7}||ADT^A03|MSGID1235|P|2.5`,
    `PID|1||${f.mrn}^^^HOSPITAL^MR||${f.lastName}^${f.firstName}^^^^^L||${pid7}|${f.gender}`,
    `PV1|1|I|WARD^ROOM^BED|||||||||||||||${f.visitNumber}|||||||||||||||||||||||||${pv1_45}`,
  ].join("\n");
}

function buildOruR01(f: Hl7FormState): string {
  const msh7 = toHl7Ts(f.obsDateTime) || "202501011300";
  const pid7 = toHl7Date(f.birthDate);
  const obx14 = toHl7Ts(f.obsDateTime);

  return [
    `MSH|^~\\&|LAB_APP|LAB_FAC|RECEIVING_APP|RECEIVING_FAC|${msh7}||ORU^R01|MSGID2001|P|2.5`,
    `PID|1||${f.mrn}^^^HOSPITAL^MR||${f.lastName}^${f.firstName}^^^^^L||${pid7}|${f.gender}`,
    `PV1|1|I|WARD^ROOM^BED|||||||||||||||${f.visitNumber}`,
    `OBR|1|||${f.obsCode}^${f.obsDisplay} TEST`,
    `OBX|1|NM|${f.obsCode}^${f.obsDisplay}||${f.obsValue}|${f.obsUnit}|70-110|N|||F|||${obx14}`,
  ].join("\n");
}

export default function Hl7SimulatorPage() {
  const [form, setForm] = useState<Hl7FormState>(initialState);
  const [lastRequest, setLastRequest] = useState<string | null>(null);
  const [lastResponse, setLastResponse] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function update<K extends keyof Hl7FormState>(
    key: K,
    value: Hl7FormState[K]
  ) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function send(kind: "ADT_A01" | "ORU_R01" | "ADT_A03") {
    setLoading(true);
    setError(null);
    setLastResponse(null);

    const body =
      kind === "ADT_A01"
        ? buildAdtA01(form)
        : kind === "ADT_A03"
        ? buildAdtA03(form)
        : buildOruR01(form);

    try {
      setLastRequest(body);
      const json = await postHl7Message(body);
      setLastResponse(JSON.stringify(json, null, 2));
    } catch (e: any) {
      setError(e.message ?? "HL7 send failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main style={{ maxWidth: 1100, margin: "2rem auto", padding: "0 1rem" }}>
      <p>
        <Link href="/">&larr; Back to patients</Link>
      </p>
      <h1>HL7 v2 Simulator</h1>
      <p>
        Send customizable ADT^A01 / ORU^R01 / ADT^A03 messages into the{" "}
        <code>/api/hl7/messages</code> endpoint.
      </p>

      {/* Controls */}
      <div
        style={{
          display: "flex",
          gap: "1rem",
          flexWrap: "wrap",
          marginTop: "1rem",
        }}
      >
        <section style={{ flex: 1, minWidth: 260 }}>
          <h3>Patient</h3>
          <label>
            MRN
            <input
              value={form.mrn}
              onChange={(e) => update("mrn", e.target.value)}
            />
          </label>
          <label>
            First name
            <input
              value={form.firstName}
              onChange={(e) => update("firstName", e.target.value)}
            />
          </label>
          <label>
            Last name
            <input
              value={form.lastName}
              onChange={(e) => update("lastName", e.target.value)}
            />
          </label>
          <label>
            Birth date (YYYY-MM-DD)
            <input
              value={form.birthDate}
              onChange={(e) => update("birthDate", e.target.value)}
            />
          </label>
          <label>
            Gender
            <select
              value={form.gender}
              onChange={(e) =>
                update("gender", e.target.value as Hl7FormState["gender"])
              }
            >
              <option value="M">M</option>
              <option value="F">F</option>
              <option value="U">U</option>
            </select>
          </label>
        </section>

        <section style={{ flex: 1, minWidth: 260 }}>
          <h3>Encounter</h3>
          <label>
            Visit number
            <input
              value={form.visitNumber}
              onChange={(e) => update("visitNumber", e.target.value)}
            />
          </label>
          <label>
            Admit date/time
            <input
              type="datetime-local"
              value={form.admitDateTime}
              onChange={(e) => update("admitDateTime", e.target.value)}
            />
          </label>
          <label>
            Discharge date/time
            <input
              type="datetime-local"
              value={form.dischargeDateTime}
              onChange={(e) => update("dischargeDateTime", e.target.value)}
            />
          </label>
        </section>

        <section style={{ flex: 1, minWidth: 260 }}>
          <h3>Observation (ORU^R01)</h3>
          <label>
            Code
            <input
              value={form.obsCode}
              onChange={(e) => update("obsCode", e.target.value)}
            />
          </label>
          <label>
            Display
            <input
              value={form.obsDisplay}
              onChange={(e) => update("obsDisplay", e.target.value)}
            />
          </label>
          <label>
            Value
            <input
              value={form.obsValue}
              onChange={(e) => update("obsValue", e.target.value)}
            />
          </label>
          <label>
            Unit
            <input
              value={form.obsUnit}
              onChange={(e) => update("obsUnit", e.target.value)}
            />
          </label>
          <label>
            Observation date/time
            <input
              type="datetime-local"
              value={form.obsDateTime}
              onChange={(e) => update("obsDateTime", e.target.value)}
            />
          </label>
        </section>
      </div>

      {/* Buttons */}
      <div style={{ display: "flex", gap: "0.5rem", margin: "1.5rem 0" }}>
        <button disabled={loading} onClick={() => send("ADT_A01")}>
          Send ADT^A01 (Admit)
        </button>
        <button disabled={loading} onClick={() => send("ORU_R01")}>
          Send ORU^R01 (Lab Result)
        </button>
        <button disabled={loading} onClick={() => send("ADT_A03")}>
          Send ADT^A03 (Discharge)
        </button>
      </div>

      {loading && <p>Sending...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {/* Display last request/response */}
      <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
        <div style={{ flex: 1 }}>
          <h3>Last HL7 Message</h3>
          <pre
            style={{
              backgroundColor: "#f3f4f6",
              padding: "0.75rem",
              borderRadius: 4,
              whiteSpace: "pre-wrap",
            }}
          >
            {lastRequest ?? "(none yet)"}
          </pre>
        </div>
        <div style={{ flex: 1 }}>
          <h3>Last Response</h3>
          <pre
            style={{
              backgroundColor: "#f3f4f6",
              padding: "0.75rem",
              borderRadius: 4,
              whiteSpace: "pre-wrap",
            }}
          >
            {lastResponse ?? "(none yet)"}
          </pre>
        </div>
      </div>

      <style jsx>{`
        label {
          display: flex;
          flex-direction: column;
          font-size: 0.85rem;
          margin-bottom: 0.5rem;
        }
        input,
        select {
          margin-top: 0.1rem;
          padding: 0.25rem 0.4rem;
          border-radius: 4px;
          border: 1px solid #d1d5db;
        }
        button {
          padding: 0.35rem 0.75rem;
          border-radius: 4px;
          border: 1px solid #d1d5db;
          background-color: #f9fafb;
          cursor: pointer;
        }
        button:disabled {
          opacity: 0.6;
          cursor: default;
        }
      `}</style>
    </main>
  );
}
