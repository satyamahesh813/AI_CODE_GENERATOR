"use client";

import { PromptInput } from "@/components/PromptInput";
import { ConfigPanel } from "@/components/ConfigPanel";
import { FilePreview } from "@/components/FilePreview";
import { useStore } from "@/store/useStore";
import { Loader2, Play, Terminal, CheckCircle2 } from "lucide-react";
import axios from "axios";
import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react";

export default function Home() {
  const { prompt, generatedFiles, setGeneratedFiles, isGenerating, setIsGenerating, currentJobId, setCurrentJobId, setJobError } = useStore();
  const [jobStatus, setJobStatus] = useState<string | null>(null);

  // Loading Steps for visual feedback
  const [loadingStep, setLoadingStep] = useState(0);
  const loadingMessages = [
    "Analyzing Intent...",
    "Architecting Microservice...",
    "Generating Components...",
    "Enforcing Governance...",
    "Finalizing Artifacts..."
  ];

  const handleManualGenerate = async () => {
    // Clear previous generation state
    setGeneratedFiles({});
    setCurrentJobId(null);
    setJobError(null);

    setIsGenerating(true);
    setJobStatus("GENERATING");

    // Simulate steps for UI effect
    setLoadingStep(0);
    const interval = setInterval(() => {
      setLoadingStep((prev) => (prev + 1) % loadingMessages.length);
    }, 1500);

    try {
      const response = await axios.post("http://localhost:8081/api/generate", { prompt });
      const job = response.data;
      setCurrentJobId(job.id);
      setJobStatus(job.status);

      clearInterval(interval);

      if (job.status === "COMPLETED") {
        setGeneratedFiles(job.generatedFiles || {});
        setTimeout(() => {
          setIsGenerating(false);
        }, 800);
      } else {
        setIsGenerating(false);
        setGeneratedFiles({});
        if (job.status === "FAILED") {
          setJobError(job.error || "Synthesis failed due to LLM provider error.");
        }
      }
    } catch (error) {
      clearInterval(interval);
      console.error("Generation failed", error);
      setJobStatus("ERROR");
      setIsGenerating(false);
    }
  };

  const handleDownloadZip = () => {
    if (!currentJobId) return;
    window.location.href = `http://localhost:8081/api/download/${currentJobId}`;
  };

  return (
    <main className="h-screen bg-slate-50 text-slate-900 overflow-hidden flex flex-col font-sans">

      {/* Top Navigation Bar - Simplified */}
      <nav className="h-14 border-b border-slate-200 bg-white flex items-center justify-between px-6 shrink-0 z-30">
        <div className="flex items-center gap-2">
          <Terminal size={18} className="text-blue-600" />
          <h1 className="text-sm font-bold tracking-tight text-slate-900 uppercase">
            MicroGen Architect
          </h1>
        </div>
      </nav>

      {/* Main Content - Split Layout */}
      <div className="flex-1 flex overflow-hidden">

        {/* LEFT PANEL: Input & Configuration */}
        <div className="w-[340px] flex flex-col border-r border-slate-200 bg-white shrink-0">
          <div className="flex-1 overflow-y-auto p-6 space-y-6">

            {/* Input Component (Header removed) */}
            <div className="space-y-4">
              <PromptInput />
            </div>

            {/* Main Action */}
            <button
              onClick={handleManualGenerate}
              disabled={isGenerating || !prompt}
              className="w-full h-12 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-lg flex items-center justify-center gap-2 transition-colors shadow-sm"
            >
              {isGenerating ? (
                <>
                  <Loader2 size={16} className="animate-spin text-white" />
                  <span>Processing...</span>
                </>
              ) : (
                <>
                  <Play size={16} className="fill-white" />
                  <span>Generate</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* RIGHT PANEL: Preview / Results */}
        <div className="flex-1 flex flex-col bg-slate-50/50 relative min-w-0">
          {/* Action Header (If Completed) */}
          {currentJobId && jobStatus === "COMPLETED" && (
            <div className="h-14 border-b border-slate-200 bg-white px-6 flex items-center justify-between shrink-0">
              <div className="flex items-center gap-2 text-emerald-600 font-medium text-xs uppercase tracking-wider">
                <CheckCircle2 size={16} />
                Generation Complete
              </div>
              <button
                onClick={handleDownloadZip}
                className="px-4 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-md shadow-sm transition-colors"
              >
                Download Bundle
              </button>
            </div>
          )}

          <div className="flex-1 overflow-hidden p-6">
            <FilePreview />
          </div>
        </div>

      </div>

      {/* Fullscreen Loading Overlay - Fixed Z-Index and Background */}
      <AnimatePresence>
        {isGenerating && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed top-0 left-0 w-screen h-screen z-[9999] bg-white opacity-100 flex flex-col items-center justify-center p-4 text-center"
          >
            <div className="mb-8">
              <Loader2 size={40} className="text-blue-600 animate-spin" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 mb-2">Synthesizing Architecture</h3>
            <p className="text-slate-500 font-medium text-sm">
              {loadingMessages[loadingStep]}
            </p>

            <div className="w-64 h-1 bg-slate-100 rounded-full mt-8 overflow-hidden">
              <motion.div
                layoutId="loading-bar"
                className="h-full bg-blue-600"
                initial={{ width: "0%" }}
                animate={{ width: "100%" }}
                transition={{ duration: 8, repeat: Infinity }}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

    </main>
  );
}
