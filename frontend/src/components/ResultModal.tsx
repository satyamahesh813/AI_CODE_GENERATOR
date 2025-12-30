"use client";

import { motion, AnimatePresence } from "framer-motion";
import { Download, X, Code2, FolderTree, FileCode, CheckCircle2 } from "lucide-react";

interface ResultModalProps {
    isOpen: boolean;
    onClose: () => void;
    onDownload: () => void;
    generatedFiles: Record<string, string>;
    jobId: string | null;
}

export function ResultModal({ isOpen, onClose, onDownload, generatedFiles, jobId }: ResultModalProps) {
    if (!isOpen) return null;

    const fileCount = Object.keys(generatedFiles).length;

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
            >
                <motion.div
                    initial={{ scale: 0.95, opacity: 0, y: 20 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.95, opacity: 0, y: 20 }}
                    className="relative w-full max-w-2xl bg-[#0f172a] border border-blue-500/20 rounded-3xl shadow-[0_0_50px_rgba(37,99,235,0.1)] overflow-hidden"
                >
                    {/* Header */}
                    <div className="flex items-center justify-between p-8 border-b border-white/5 bg-white/[0.02]">
                        <div className="flex items-center gap-4">
                            <div className="h-12 w-12 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                                <CheckCircle2 size={24} className="text-white" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-bold text-white tracking-tight">System Synthesized</h3>
                                <p className="text-slate-400 text-sm font-medium">Ready for deployment</p>
                            </div>
                        </div>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-white/5 rounded-xl transition-colors text-slate-400 hover:text-white"
                        >
                            <X size={20} />
                        </button>
                    </div>

                    {/* Content */}
                    <div className="p-8 space-y-8">
                        <div className="grid grid-cols-2 gap-4">
                            <div className="p-4 rounded-2xl bg-blue-500/5 border border-blue-500/10 flex flex-col gap-2">
                                <div className="flex items-center gap-2 text-blue-400">
                                    <FolderTree size={18} />
                                    <span className="text-xs font-bold uppercase tracking-wider">Structure</span>
                                </div>
                                <span className="text-2xl font-black text-white">Microservice</span>
                            </div>
                            <div className="p-4 rounded-2xl bg-indigo-500/5 border border-indigo-500/10 flex flex-col gap-2">
                                <div className="flex items-center gap-2 text-indigo-400">
                                    <FileCode size={18} />
                                    <span className="text-xs font-bold uppercase tracking-wider">Components</span>
                                </div>
                                <span className="text-2xl font-black text-white">{fileCount} <span className="text-sm font-medium text-slate-500">Files</span></span>
                            </div>
                        </div>

                        <div className="p-6 rounded-2xl bg-slate-950 border border-white/5 space-y-4">
                            <h4 className="text-sm font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                                <Code2 size={14} />
                                Generated Artifacts
                            </h4>
                            <div className="max-h-48 overflow-y-auto pr-2 space-y-1 scrollbar-thin scrollbar-thumb-white/10 scrollbar-track-transparent">
                                {Object.keys(generatedFiles).map((file, i) => (
                                    <div key={i} className="flex items-center gap-3 text-sm text-slate-400 py-2 border-b border-white/[0.04] last:border-0 hover:text-blue-300 transition-colors">
                                        <span className="w-1.5 h-1.5 rounded-full bg-blue-500/50" />
                                        {file}
                                    </div>
                                ))}
                            </div>
                        </div>

                        <button
                            onClick={onDownload}
                            className="w-full py-5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold text-lg uppercase tracking-widest rounded-2xl transition-all shadow-lg shadow-blue-500/25 active:scale-[0.98] flex items-center justify-center gap-3"
                        >
                            <Download size={20} />
                            Download Project Bundle
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}
