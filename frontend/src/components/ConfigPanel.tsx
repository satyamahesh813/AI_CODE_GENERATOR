"use client";

import { useStore } from "@/store/useStore";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Shield, Database, Layers, Radio, Zap, Package, Activity, GitBranch, Code2, ChevronDown, ChevronUp } from "lucide-react";

type Option = { value: string; label: string; desc: string };
type Section = { key: string; label: string; icon: React.ElementType; color: string; bg: string; options: Option[] };

const SECTIONS: Section[] = [
  { key: "auth", label: "Auth Strategy", icon: Shield, color: "#4f46e5", bg: "#eef2ff",
    options: [{ value: "NONE", label: "None", desc: "No auth" }, { value: "JWT", label: "JWT", desc: "Stateless" }, { value: "OAUTH2", label: "OAuth2", desc: "Social / SSO" }, { value: "BASIC", label: "Basic", desc: "HTTP Basic" }, { value: "API_KEY", label: "API Key", desc: "Header key" }] },
  { key: "database", label: "Database", icon: Database, color: "#0891b2", bg: "#ecfeff",
    options: [{ value: "NONE", label: "None", desc: "No DB" }, { value: "H2", label: "H2", desc: "In-memory" }, { value: "MYSQL", label: "MySQL", desc: "Relational" }, { value: "POSTGRESQL", label: "Postgres", desc: "Advanced SQL" }, { value: "MONGODB", label: "MongoDB", desc: "Document DB" }, { value: "REDIS", label: "Redis", desc: "Key-value" }] },
  { key: "persistence", label: "Persistence", icon: Layers, color: "#7c3aed", bg: "#f5f3ff",
    options: [{ value: "NONE", label: "None", desc: "No ORM" }, { value: "JPA", label: "JPA", desc: "Hibernate" }, { value: "MYBATIS", label: "MyBatis", desc: "SQL mapper" }, { value: "R2DBC", label: "R2DBC", desc: "Reactive" }] },
  { key: "messaging", label: "Messaging", icon: Radio, color: "#d97706", bg: "#fffbeb",
    options: [{ value: "NONE", label: "None", desc: "No messaging" }, { value: "KAFKA", label: "Kafka", desc: "Event stream" }, { value: "RABBITMQ", label: "RabbitMQ", desc: "AMQP broker" }, { value: "SQS", label: "AWS SQS", desc: "Managed queue" }, { value: "ACTIVEMQ", label: "ActiveMQ", desc: "JMS broker" }] },
  { key: "cache", label: "Caching", icon: Zap, color: "#059669", bg: "#ecfdf5",
    options: [{ value: "NONE", label: "None", desc: "No cache" }, { value: "REDIS", label: "Redis", desc: "Distributed" }, { value: "CAFFEINE", label: "Caffeine", desc: "In-process" }, { value: "HAZELCAST", label: "Hazelcast", desc: "Clustered" }] },
  { key: "observability", label: "Observability", icon: Activity, color: "#dc2626", bg: "#fef2f2",
    options: [{ value: "NONE", label: "None", desc: "No monitoring" }, { value: "ACTUATOR", label: "Actuator", desc: "Health + metrics" }, { value: "PROMETHEUS", label: "Prometheus", desc: "Metrics scrape" }, { value: "ZIPKIN", label: "Zipkin", desc: "Distributed trace" }, { value: "OPENTELEMETRY", label: "OTel", desc: "Full telemetry" }] },
  { key: "architecture", label: "Architecture", icon: GitBranch, color: "#0f172a", bg: "#f8fafc",
    options: [{ value: "LAYERED", label: "Layered", desc: "MVC / N-tier" }, { value: "HEXAGONAL", label: "Hexagonal", desc: "Ports & adapters" }, { value: "CQRS", label: "CQRS", desc: "Command / Query" }, { value: "EVENT_DRIVEN", label: "Event", desc: "Event-driven" }] },
  { key: "buildTool", label: "Build Tool", icon: Package, color: "#b45309", bg: "#fef3c7",
    options: [{ value: "MAVEN", label: "Maven", desc: "pom.xml" }, { value: "GRADLE", label: "Gradle", desc: "build.gradle" }] },
  { key: "language", label: "Language", icon: Code2, color: "#1d4ed8", bg: "#eff6ff",
    options: [{ value: "JAVA", label: "Java 21", desc: "LTS" }, { value: "KOTLIN", label: "Kotlin", desc: "JVM modern" }] },
];

function SectionRow({ section, selected, onSelect, index }: {
  section: Section; selected: string; onSelect: (v: string) => void; index: number;
}) {
  const { key, label, icon: Icon, color, bg, options } = section;

  return (
    <motion.div
      initial={{ opacity: 0, x: -12 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05 }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 7 }}>
        <motion.div
          whileHover={{ scale: 1.15, rotate: 8 }}
          style={{ width: 20, height: 20, borderRadius: 6, background: bg, display: "flex", alignItems: "center", justifyContent: "center", border: `1px solid ${color}22` }}
        >
          <Icon size={11} color={color} />
        </motion.div>
        <span style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>{label}</span>
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 5 }}>
        {options.map(({ value, label: optLabel, desc }) => {
          const isSelected = selected === value;
          return (
            <motion.button
              key={value}
              onClick={() => onSelect(value)}
              whileHover={{ scale: 1.04, y: -1 }}
              whileTap={{ scale: 0.96 }}
              title={desc}
              style={{
                padding: "6px 10px", borderRadius: 8,
                border: `2px solid ${isSelected ? color : "#e2e8f0"}`,
                background: isSelected ? bg : "white",
                cursor: "pointer", fontFamily: "inherit",
                boxShadow: isSelected ? `0 0 0 3px ${color}22, 0 2px 8px ${color}18` : "none",
                transition: "border-color 0.15s, background 0.15s, box-shadow 0.15s",
                minWidth: 52, position: "relative", overflow: "hidden",
              }}
            >
              {/* Selected shimmer */}
              {isSelected && (
                <motion.div
                  style={{
                    position: "absolute", inset: 0,
                    background: `linear-gradient(90deg, transparent, ${color}15, transparent)`,
                  }}
                  animate={{ x: ["-100%", "200%"] }}
                  transition={{ duration: 2, repeat: Infinity, ease: "linear", repeatDelay: 2 }}
                />
              )}
              <div style={{ fontSize: 11, fontWeight: 700, color: isSelected ? color : "#374151" }}>{optLabel}</div>
              <div style={{ fontSize: 9, color: isSelected ? color : "#9ca3af", marginTop: 1 }}>{desc}</div>
            </motion.button>
          );
        })}
      </div>
    </motion.div>
  );
}

export const ConfigPanel = () => {
  const { config, setConfig } = useStore();
  const [expanded, setExpanded] = useState(false);

  const primary = SECTIONS.slice(0, 4);
  const advanced = SECTIONS.slice(4);

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.25, type: "spring", stiffness: 300, damping: 28 }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 14 }}>
        <motion.div
          whileHover={{ scale: 1.1, rotate: 5 }}
          style={{ width: 22, height: 22, borderRadius: 6, background: "linear-gradient(135deg, #4f46e5, #7c3aed)", display: "flex", alignItems: "center", justifyContent: "center" }}
        >
          <span style={{ fontSize: 11, color: "white", fontWeight: 700 }}>2</span>
        </motion.div>
        <span style={{ fontSize: 13, fontWeight: 700, color: "#0f172a" }}>Configure options</span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
        {primary.map((s, i) => (
          <SectionRow key={s.key} section={s} index={i}
            selected={(config as Record<string, string>)[s.key]}
            onSelect={(v) => setConfig({ [s.key]: v })}
          />
        ))}

        {/* Advanced toggle */}
        <motion.button
          onClick={() => setExpanded((e) => !e)}
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.98 }}
          style={{
            display: "flex", alignItems: "center", justifyContent: "center", gap: 5,
            padding: "8px 12px", borderRadius: 10,
            border: "1px dashed #c7d2fe", background: expanded ? "#eef2ff" : "white",
            cursor: "pointer", fontFamily: "inherit",
            fontSize: 12, fontWeight: 600, color: "#4f46e5",
            transition: "background 0.15s",
          }}
        >
          <motion.div animate={{ rotate: expanded ? 180 : 0 }} transition={{ type: "spring", stiffness: 300 }}>
            <ChevronDown size={13} />
          </motion.div>
          {expanded ? "Hide advanced options" : "Show advanced options"}
          <motion.span
            animate={{ scale: expanded ? 1.1 : 1 }}
            style={{ fontSize: 10, padding: "1px 6px", borderRadius: 99, background: "#e0e7ff", color: "#4f46e5", fontWeight: 700 }}
          >
            {advanced.length}
          </motion.span>
        </motion.button>

        <AnimatePresence>
          {expanded && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
              style={{ overflow: "hidden", display: "flex", flexDirection: "column", gap: 14 }}
            >
              {advanced.map((s, i) => (
                <SectionRow key={s.key} section={s} index={i}
                  selected={(config as Record<string, string>)[s.key]}
                  onSelect={(v) => setConfig({ [s.key]: v })}
                />
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
};
