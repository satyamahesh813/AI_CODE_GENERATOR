"use client";

import { useStore } from "@/store/useStore";
import Editor from "@monaco-editor/react";
import { useState, useEffect } from "react";
import { FileCode, ChevronRight, File, Folder, Terminal } from "lucide-react";
import { motion } from "framer-motion";

export const FilePreview = () => {
    const { generatedFiles, isGenerating } = useStore();
    const [selectedFile, setSelectedFile] = useState<string | null>(null);

    useEffect(() => {
        const files = Object.keys(generatedFiles);
        if (files.length > 0) {
            if (!selectedFile || !generatedFiles[selectedFile]) {
                setSelectedFile(files.sort()[0]);
            }
        } else {
            setSelectedFile(null);
        }
    }, [generatedFiles, selectedFile]);

    if (Object.keys(generatedFiles).length === 0) {
        if (isGenerating) return null;
        return (
            <div className="h-full flex flex-col items-center justify-center p-12 text-center bg-slate-50 border border-slate-200 rounded-2xl border-dashed">
                <div className="bg-white p-6 rounded-full shadow-sm mb-6 border border-slate-100">
                    <Terminal size={32} className="text-slate-400" />
                </div>
                <h4 className="text-lg font-bold text-slate-700 mb-2">Ready to Architect</h4>
                <p className="text-slate-500 text-sm max-w-sm">
                    Generated source code will appear here. Run the synthesis engine to begin.
                </p>
            </div>
        );
    }

    const fileList = Object.keys(generatedFiles).sort();

    return (
        <div className="h-full flex overflow-hidden border border-slate-200 rounded-2xl bg-white shadow-sm">
            {/* Sidebar */}
            <div className="w-72 border-r border-slate-200 flex flex-col bg-slate-50">
                <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-white">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Project Files</span>
                    <Folder size={14} className="text-slate-400" />
                </div>
                <div className="flex-1 overflow-y-auto p-3 space-y-1">
                    {fileList.map((file) => (
                        <button
                            key={file}
                            onClick={() => setSelectedFile(file)}
                            className={`w-full text-left px-3 py-2.5 rounded-lg text-xs font-medium transition-all flex items-center justify-between group relative ${selectedFile === file
                                ? "bg-white text-blue-600 shadow-sm ring-1 ring-slate-200"
                                : "text-slate-600 hover:bg-white hover:text-slate-900"
                                }`}
                        >
                            <div className="flex items-center gap-2.5 overflow-hidden w-full">
                                <File size={14} className={selectedFile === file ? "text-blue-500" : "text-slate-400"} />
                                <span className="truncate">{file.split("/").pop()}</span>
                            </div>
                        </button>
                    ))}
                </div>
            </div>

            {/* Editor Container */}
            <div className="flex-1 flex flex-col bg-white">
                <div className="h-12 px-6 border-b border-slate-200 flex items-center justify-between bg-white">
                    <div className="flex items-center gap-2">
                        <FileCode size={14} className="text-slate-400" />
                        <span className="text-xs font-semibold text-slate-700 truncate max-w-lg">{selectedFile}</span>
                    </div>
                </div>

                <div className="flex-1 relative">
                    <Editor
                        height="100%"
                        theme="light" // Switched to light theme
                        defaultLanguage="java"
                        path={selectedFile || ""}
                        value={selectedFile ? generatedFiles[selectedFile] : ""}
                        options={{
                            readOnly: true,
                            minimap: { enabled: false },
                            fontSize: 13,
                            fontFamily: "'JetBrains Mono', monospace",
                            padding: { top: 24, bottom: 24 },
                            scrollBeyondLastLine: false,
                            lineNumbersMinChars: 3,
                            renderLineHighlight: 'all',
                            scrollbar: {
                                vertical: 'visible',
                                horizontal: 'visible'
                            },
                        }}
                    />
                </div>
            </div>
        </div>
    );
};
