"use client";

import { useStore } from "@/store/useStore";
import { motion, AnimatePresence } from "framer-motion";
import { Lightbulb, ArrowRight } from "lucide-react";
import { useState } from "react";

const EXAMPLES = [
  "Order service with PostgreSQL, JPA and JWT auth",
  "Kafka consumer with dead-letter queue support",
  "User auth service with OAuth2 and MySQL",
  "Inventory service with Redis caching and REST API",
];

export const PromptInput = () => {
  const { prompt, setPrompt } = useStore();
  const [focused, setFocused] = useState(false);
  const [hoveredExample, setHoveredExample] = useState<number | null>(null);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.15, type: "spring", stiffness: 300, damping: 28 }}
    >
      {/* Label */}
      <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 10 }}>
        <motion.div
          whileHover={{ scale: 1.1, rotate: 5 }}
          style={{
            width: 22, height: 22, borderRadius: 6,
            background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}
        >
          <span style={{ fontSize: 11, color: "white", fontWeight: 700 }}>1</span>
        </motion.div>
        <span style={{ fontSize: 13, fontWeight: 700, color: "#0f172a" }}>Describe your microservice</span>
      </div>

      {/* Textarea */}
      <motion.div
        animate={{
          boxShadow: focused
            ? "0 0 0 3px rgba(79,70,229,0.15), 0 4px 16px rgba(79,70,229,0.1)"
            : "0 1px 4px rgba(0,0,0,0.06)",
        }}
        style={{ borderRadius: 14, position: "relative" }}
      >
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          placeholder="e.g. Order service with PostgreSQL, Kafka events, and JWT authentication..."
          rows={5}
          style={{
            width: "100%", padding: "14px 16px", borderRadius: 14,
            border: `2px solid ${focused ? "#a5b4fc" : prompt ? "#c4b5fd" : "#e2e8f0"}`,
            background: focused ? "#fafafe" : "white",
            fontSize: 13, color: "#0f172a", lineHeight: 1.6,
            resize: "none", outline: "none", fontFamily: "inherit",
            transition: "border-color 0.2s, background 0.2s",
            boxSizing: "border-box",
          }}
        />

        {/* Char count */}
        <AnimatePresence>
          {prompt && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              style={{
                position: "absolute", bottom: 10, right: 12,
                fontSize: 10, color: prompt.length > 3500 ? "#ef4444" : "#94a3b8",
                fontWeight: 500, background: "white", padding: "1px 6px", borderRadius: 99,
              }}
            >
              {prompt.length}/4000
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      {/* Examples */}
      <div style={{ marginTop: 14 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 5, marginBottom: 8 }}>
          <motion.div animate={{ rotate: [0, 15, 0] }} transition={{ duration: 2, repeat: Infinity, repeatDelay: 3 }}>
            <Lightbulb size={12} color="#f59e0b" />
          </motion.div>
          <span style={{ fontSize: 11, fontWeight: 600, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Quick examples
          </span>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
          {EXAMPLES.map((ex, i) => (
            <motion.button
              key={ex}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 + i * 0.06 }}
              whileHover={{ x: 4, background: "#fafafe" }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setPrompt(ex)}
              onMouseEnter={() => setHoveredExample(i)}
              onMouseLeave={() => setHoveredExample(null)}
              style={{
                textAlign: "left", padding: "8px 12px", borderRadius: 10,
                border: `1px solid ${hoveredExample === i ? "#a5b4fc" : "#e2e8f0"}`,
                background: hoveredExample === i ? "#fafafe" : "white",
                fontSize: 12, color: hoveredExample === i ? "#4f46e5" : "#475569",
                cursor: "pointer", fontFamily: "inherit",
                display: "flex", alignItems: "center", justifyContent: "space-between",
                transition: "border-color 0.15s, color 0.15s",
              }}
            >
              <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", flex: 1 }}>
                {ex}
              </span>
              <AnimatePresence>
                {hoveredExample === i && (
                  <motion.div
                    initial={{ opacity: 0, x: -4 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -4 }}
                    style={{ flexShrink: 0, marginLeft: 6 }}
                  >
                    <ArrowRight size={12} color="#4f46e5" />
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.button>
          ))}
        </div>
      </div>
    </motion.div>
  );
};
