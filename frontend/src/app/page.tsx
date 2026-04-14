"use client";

import { PromptInput } from "@/components/PromptInput";
import { ConfigPanel } from "@/components/ConfigPanel";
import { FilePreview } from "@/components/FilePreview";
import { ErrorToast } from "@/components/ErrorToast";
import { useStore } from "@/store/useStore";
import { Sparkles, CheckCircle2, Download, RotateCcw, Zap, Code2, Cpu, GitBranch, Package } from "lucide-react";
import axios from "axios";
import { motion, AnimatePresence, useMotionValue, useTransform } from "framer-motion";
import { useState, useEffect } from "react";

const LOADING_STEPS = [
  { label: "Analyzing your intent...",      icon: "🧠", color: "#6366f1" },
  { label: "Designing the architecture...", icon: "🏗️", color: "#8b5cf6" },
  { label: "Generating components...",      icon: "⚙️", color: "#a855f7" },
  { label: "Running governance checks...",  icon: "🛡️", color: "#7c3aed" },
  { label: "Packaging artifacts...",        icon: "📦", color: "#4f46e5" },
];

const FLOATING_ICONS = [Code2, Cpu, GitBranch, Package, Zap];

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081";

export default function Home() {
  const {
    prompt, config,
    generatedFiles, setGeneratedFiles,
    isGenerating, setIsGenerating,
    currentJobId, setCurrentJobId,
    jobError, setJobError,
    reset,
  } = useStore();

  const [jobStatus, setJobStatus] = useState<string | null>(null);
  const [loadingStep, setLoadingStep] = useState(0);
  const [particles, setParticles] = useState<{ id: number; x: number; y: number; size: number; delay: number }[]>([]);

  // Generate floating particles on mount
  useEffect(() => {
    setParticles(
      Array.from({ length: 18 }, (_, i) => ({
        id: i,
        x: Math.random() * 100,
        y: Math.random() * 100,
        size: Math.random() * 4 + 2,
        delay: Math.random() * 4,
      }))
    );
  }, []);

  const handleGenerate = async () => {
    reset();
    setIsGenerating(true);
    setJobStatus("GENERATING");
    setLoadingStep(0);

    const interval = setInterval(() => {
      setLoadingStep((p) => (p + 1) % LOADING_STEPS.length);
    }, 2000);

    try {
      const { data: job } = await axios.post(`${API_BASE}/api/generate`, {
        prompt,
        auth: config.auth, database: config.database, persistence: config.persistence,
        messaging: config.messaging, cache: config.cache, buildTool: config.buildTool,
        observability: config.observability, architecture: config.architecture, language: config.language,
      });

      clearInterval(interval);
      setCurrentJobId(job.id);
      setJobStatus(job.status);

      if (job.status === "COMPLETED") {
        setGeneratedFiles(job.generatedFiles ?? {});
        setTimeout(() => setIsGenerating(false), 500);
      } else {
        setIsGenerating(false);
        setJobError(
          job.error ?? (job.status === "FAILED_GOVERNANCE"
            ? "Blocked by governance scan — potential secrets detected."
            : "Generation failed. Try rephrasing your prompt.")
        );
      }
    } catch (err: unknown) {
      clearInterval(interval);
      setJobStatus("ERROR");
      setIsGenerating(false);
      if (axios.isAxiosError(err)) {
        setJobError(`Request failed: ${err.response?.data?.message ?? err.message}`);
      } else {
        setJobError("Unexpected error. Is the backend running on port 8081?");
      }
    }
  };

  const handleDownload = () => {
    if (currentJobId) window.location.href = `${API_BASE}/api/download/${currentJobId}`;
  };

  const hasFiles = Object.keys(generatedFiles).length > 0;
  const isComplete = jobStatus === "COMPLETED" && hasFiles;
  const step = LOADING_STEPS[loadingStep];

  return (
    <div style={{
      height: "100vh", display: "flex", flexDirection: "column",
      background: "linear-gradient(135deg, #f0f4ff 0%, #faf5ff 50%, #f0fdf4 100%)",
      overflow: "hidden", position: "relative",
    }}>

      {/* Ambient floating particles */}
      {particles.map((p) => (
        <motion.div
          key={p.id}
          style={{
            position: "fixed", left: `${p.x}%`, top: `${p.y}%`,
            width: p.size, height: p.size, borderRadius: "50%",
            background: "linear-gradient(135deg, #a5b4fc, #c4b5fd)",
            pointerEvents: "none", zIndex: 0, opacity: 0.4,
          }}
          animate={{ y: [-10, 10, -10], x: [-5, 5, -5], opacity: [0.2, 0.5, 0.2] }}
          transition={{ duration: 4 + p.delay, repeat: Infinity, ease: "easeInOut", delay: p.delay }}
        />
      ))}

      {/* Error toast */}
      <ErrorToast error={jobError} onDismiss={() => setJobError(null)} onRetry={jobError ? handleGenerate : undefined} />

      {/* NAV */}
      <motion.nav
        initial={{ y: -60, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        style={{
          height: 60, background: "rgba(255,255,255,0.85)", backdropFilter: "blur(20px)",
          borderBottom: "1px solid #e2e8f0", display: "flex", alignItems: "center",
          justifyContent: "space-between", padding: "0 24px", flexShrink: 0, zIndex: 50,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <motion.div
            whileHover={{ rotate: 15, scale: 1.1 }}
            transition={{ type: "spring", stiffness: 400 }}
            style={{
              width: 36, height: 36, borderRadius: 10,
              background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
              display: "flex", alignItems: "center", justifyContent: "center",
              boxShadow: "0 4px 14px rgba(79,70,229,0.4)",
            }}
          >
            <Zap size={17} color="white" fill="white" />
          </motion.div>
          <div>
            <div style={{ fontSize: 15, fontWeight: 800, color: "#0f172a", lineHeight: 1.2, letterSpacing: "-0.3px" }}>
              MicroGen Architect
            </div>
            <div style={{ fontSize: 11, color: "#94a3b8", fontWeight: 500 }}>AI-Powered Microservice Generator</div>
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <AnimatePresence>
            {isComplete && (
              <motion.button
                initial={{ opacity: 0, scale: 0.8, x: 20 }}
                animate={{ opacity: 1, scale: 1, x: 0 }}
                exit={{ opacity: 0, scale: 0.8, x: 20 }}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => { reset(); setJobStatus(null); }}
                style={{
                  display: "flex", alignItems: "center", gap: 6,
                  padding: "6px 14px", borderRadius: 8, border: "1px solid #e2e8f0",
                  background: "white", color: "#64748b", fontSize: 13, fontWeight: 500, cursor: "pointer",
                }}
              >
                <RotateCcw size={13} /> New Generation
              </motion.button>
            )}
          </AnimatePresence>

          {/* Pulsing live indicator */}
          <div style={{ display: "flex", alignItems: "center", gap: 6, padding: "4px 12px", borderRadius: 99, background: "#f0fdf4", border: "1px solid #bbf7d0" }}>
            <motion.div
              animate={{ scale: [1, 1.4, 1], opacity: [1, 0.5, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
              style={{ width: 7, height: 7, borderRadius: "50%", background: "#16a34a" }}
            />
            <span style={{ fontSize: 12, fontWeight: 600, color: "#16a34a" }}>Live</span>
          </div>
        </div>
      </motion.nav>

      {/* BODY */}
      <div style={{ flex: 1, display: "flex", overflow: "hidden", position: "relative", zIndex: 1 }}>

        {/* LEFT PANEL */}
        <motion.div
          initial={{ x: -340, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ type: "spring", stiffness: 260, damping: 28, delay: 0.1 }}
          style={{
            width: 340, display: "flex", flexDirection: "column", flexShrink: 0,
            background: "rgba(255,255,255,0.75)", backdropFilter: "blur(20px)",
            borderRight: "1px solid #e2e8f0",
          }}
        >
          <div style={{ flex: 1, overflowY: "auto", padding: "20px 20px 0" }}>
            <PromptInput />
            <div style={{ marginTop: 20 }}>
              <ConfigPanel />
            </div>
          </div>

          <div style={{ padding: 20 }}>
            <motion.button
              onClick={handleGenerate}
              disabled={isGenerating || !prompt.trim()}
              whileHover={!isGenerating && prompt.trim() ? { scale: 1.02, y: -2, boxShadow: "0 12px 32px rgba(79,70,229,0.45)" } : {}}
              whileTap={!isGenerating && prompt.trim() ? { scale: 0.97 } : {}}
              style={{
                width: "100%", height: 50, borderRadius: 14, border: "none",
                background: isGenerating || !prompt.trim()
                  ? "#e2e8f0"
                  : "linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)",
                color: isGenerating || !prompt.trim() ? "#94a3b8" : "white",
                fontSize: 15, fontWeight: 700,
                cursor: isGenerating || !prompt.trim() ? "not-allowed" : "pointer",
                display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
                boxShadow: isGenerating || !prompt.trim() ? "none" : "0 8px 24px rgba(79,70,229,0.35)",
                transition: "background 0.2s, box-shadow 0.2s",
                position: "relative", overflow: "hidden",
              }}
            >
              {/* Shimmer effect on hover */}
              {!isGenerating && prompt.trim() && (
                <motion.div
                  style={{
                    position: "absolute", inset: 0,
                    background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent)",
                    x: "-100%",
                  }}
                  animate={{ x: ["−100%", "200%"] }}
                  transition={{ duration: 2.5, repeat: Infinity, ease: "linear", repeatDelay: 1 }}
                />
              )}
              {isGenerating ? (
                <>
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                    style={{ width: 16, height: 16, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%" }}
                  />
                  Generating...
                </>
              ) : (
                <>
                  <motion.div animate={{ rotate: [0, 15, -15, 0] }} transition={{ duration: 2, repeat: Infinity, repeatDelay: 3 }}>
                    <Sparkles size={16} />
                  </motion.div>
                  Generate Microservice
                </>
              )}
            </motion.button>
          </div>
        </motion.div>

        {/* RIGHT PANEL */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}
        >
          {/* Success bar */}
          <AnimatePresence>
            {isComplete && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 52, opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ type: "spring", stiffness: 300, damping: 30 }}
                style={{
                  padding: "0 24px", flexShrink: 0, overflow: "hidden",
                  background: "linear-gradient(90deg, #f0fdf4, #dcfce7)",
                  borderBottom: "1px solid #bbf7d0",
                  display: "flex", alignItems: "center", justifyContent: "space-between",
                }}
              >
                <motion.div
                  initial={{ x: -20, opacity: 0 }}
                  animate={{ x: 0, opacity: 1 }}
                  transition={{ delay: 0.1 }}
                  style={{ display: "flex", alignItems: "center", gap: 8 }}
                >
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ type: "spring", stiffness: 500, delay: 0.15 }}
                  >
                    <CheckCircle2 size={17} color="#16a34a" />
                  </motion.div>
                  <span style={{ fontSize: 13, fontWeight: 600, color: "#15803d" }}>
                    Microservice generated — {Object.keys(generatedFiles).length} files ready
                  </span>
                </motion.div>

                <motion.button
                  initial={{ x: 20, opacity: 0 }}
                  animate={{ x: 0, opacity: 1 }}
                  transition={{ delay: 0.2 }}
                  whileHover={{ scale: 1.05, y: -1 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={handleDownload}
                  style={{
                    display: "flex", alignItems: "center", gap: 6,
                    padding: "7px 16px", borderRadius: 10, border: "none",
                    background: "linear-gradient(135deg, #16a34a, #15803d)",
                    color: "white", fontSize: 13, fontWeight: 600, cursor: "pointer",
                    boxShadow: "0 4px 12px rgba(22,163,74,0.3)",
                  }}
                >
                  <motion.div animate={{ y: [0, -2, 0] }} transition={{ duration: 1.5, repeat: Infinity }}>
                    <Download size={14} />
                  </motion.div>
                  Download Bundle
                </motion.button>
              </motion.div>
            )}
          </AnimatePresence>

          <div style={{ flex: 1, overflow: "hidden", padding: 20 }}>
            <FilePreview />
          </div>
        </motion.div>
      </div>

      {/* LOADING OVERLAY */}
      <AnimatePresence>
        {isGenerating && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: "fixed", inset: 0, zIndex: 9999,
              background: "rgba(248,250,255,0.96)", backdropFilter: "blur(16px)",
              display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 28,
            }}
          >
            {/* Orbiting icons */}
            <div style={{ position: "relative", width: 120, height: 120 }}>
              {/* Outer ring */}
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 8, repeat: Infinity, ease: "linear" }}
                style={{ position: "absolute", inset: 0 }}
              >
                {FLOATING_ICONS.map((Icon, i) => {
                  const angle = (i / FLOATING_ICONS.length) * 360;
                  const rad = (angle * Math.PI) / 180;
                  const r = 52;
                  return (
                    <motion.div
                      key={i}
                      style={{
                        position: "absolute",
                        left: 60 + r * Math.cos(rad) - 12,
                        top: 60 + r * Math.sin(rad) - 12,
                        width: 24, height: 24, borderRadius: 8,
                        background: "white",
                        border: "1px solid #e0e7ff",
                        display: "flex", alignItems: "center", justifyContent: "center",
                        boxShadow: "0 2px 8px rgba(79,70,229,0.15)",
                      }}
                      animate={{ scale: [1, 1.2, 1] }}
                      transition={{ duration: 2, repeat: Infinity, delay: i * 0.4 }}
                    >
                      <Icon size={12} color="#4f46e5" />
                    </motion.div>
                  );
                })}
              </motion.div>

              {/* Inner spinning ring */}
              <motion.div
                animate={{ rotate: -360 }}
                transition={{ duration: 3, repeat: Infinity, ease: "linear" }}
                style={{
                  position: "absolute", inset: 16, borderRadius: "50%",
                  border: "2px solid transparent",
                  borderTopColor: "#4f46e5", borderRightColor: "#a78bfa",
                  boxShadow: "0 0 20px rgba(79,70,229,0.2)",
                }}
              />

              {/* Center icon */}
              <motion.div
                animate={{ scale: [1, 1.1, 1], rotate: [0, 5, -5, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
                style={{
                  position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center",
                }}
              >
                <div style={{
                  width: 44, height: 44, borderRadius: 14,
                  background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
                  display: "flex", alignItems: "center", justifyContent: "center",
                  boxShadow: "0 8px 24px rgba(79,70,229,0.4)",
                }}>
                  <Zap size={20} color="white" fill="white" />
                </div>
              </motion.div>
            </div>

            {/* Step text */}
            <div style={{ textAlign: "center" }}>
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                style={{ fontSize: 24, fontWeight: 800, color: "#0f172a", marginBottom: 10, letterSpacing: "-0.5px" }}
              >
                Synthesizing Architecture
              </motion.div>

              <AnimatePresence mode="wait">
                <motion.div
                  key={loadingStep}
                  initial={{ opacity: 0, y: 12, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -12, scale: 0.95 }}
                  transition={{ duration: 0.3 }}
                  style={{
                    display: "inline-flex", alignItems: "center", gap: 8,
                    padding: "8px 18px", borderRadius: 99,
                    background: "white", border: "1px solid #e0e7ff",
                    boxShadow: "0 2px 12px rgba(79,70,229,0.1)",
                    fontSize: 14, fontWeight: 600, color: step.color,
                  }}
                >
                  <span style={{ fontSize: 16 }}>{step.icon}</span>
                  {step.label}
                </motion.div>
              </AnimatePresence>
            </div>

            {/* Progress bar */}
            <div style={{ width: 280, height: 5, background: "#e0e7ff", borderRadius: 99, overflow: "hidden" }}>
              <motion.div
                initial={{ width: "0%" }}
                animate={{ width: "100%" }}
                transition={{ duration: 10, ease: "linear" }}
                style={{
                  height: "100%",
                  background: "linear-gradient(90deg, #4f46e5, #7c3aed, #a855f7)",
                  borderRadius: 99,
                  boxShadow: "0 0 8px rgba(79,70,229,0.5)",
                }}
              />
            </div>

            {/* Step dots */}
            <div style={{ display: "flex", gap: 10 }}>
              {LOADING_STEPS.map((s, i) => (
                <motion.div
                  key={i}
                  animate={{
                    scale: i === loadingStep ? 1.5 : 1,
                    background: i <= loadingStep ? s.color : "#e0e7ff",
                  }}
                  transition={{ type: "spring", stiffness: 400 }}
                  style={{ width: 8, height: 8, borderRadius: "50%", background: "#e0e7ff" }}
                />
              ))}
            </div>

            {/* Floating code snippets */}
            {["@KafkaListener", "@RestController", "@Service", "@Entity", "@Transactional"].map((text, i) => (
              <motion.div
                key={text}
                style={{
                  position: "absolute",
                  left: `${10 + i * 18}%`,
                  top: `${15 + (i % 3) * 25}%`,
                  fontSize: 11, fontWeight: 600, color: "#a5b4fc",
                  fontFamily: "monospace", pointerEvents: "none",
                  background: "rgba(238,242,255,0.8)", padding: "3px 8px",
                  borderRadius: 6, border: "1px solid #e0e7ff",
                }}
                animate={{ y: [-8, 8, -8], opacity: [0.4, 0.8, 0.4] }}
                transition={{ duration: 3 + i * 0.5, repeat: Infinity, delay: i * 0.6 }}
              >
                {text}
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
