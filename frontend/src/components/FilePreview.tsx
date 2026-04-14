"use client";

import { useStore } from "@/store/useStore";
import Editor from "@monaco-editor/react";
import { useState, useEffect, useCallback } from "react";
import { FileCode, Folder, Terminal, Copy, Check, ChevronDown } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

function getLanguage(filename: string): string {
  const name = filename.toLowerCase();
  if (name.endsWith("dockerfile")) return "dockerfile";
  const ext = name.split(".").pop() ?? "";
  const map: Record<string, string> = {
    java: "java", xml: "xml", yml: "yaml", yaml: "yaml",
    properties: "ini", sql: "sql", json: "json",
    md: "markdown", sh: "shell", ts: "typescript",
    tsx: "typescript", js: "javascript", py: "python", go: "go",
  };
  return map[ext] ?? "plaintext";
}

type TreeNode = { name: string; path: string; isFile: boolean; children: TreeNode[] };

function buildTree(paths: string[]): TreeNode[] {
  const root: TreeNode[] = [];
  for (const path of paths) {
    const parts = path.split("/");
    let current = root;
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      const isFile = i === parts.length - 1;
      const fullPath = parts.slice(0, i + 1).join("/");
      let node = current.find((n) => n.name === part);
      if (!node) {
        node = { name: part, path: fullPath, isFile, children: [] };
        current.push(node);
      }
      current = node.children;
    }
  }
  return root;
}

function getFileColor(name: string): string {
  if (name.endsWith(".java")) return "#f59e0b";
  if (name.endsWith(".xml") || name === "pom.xml") return "#ef4444";
  if (name.endsWith(".yml") || name.endsWith(".yaml")) return "#10b981";
  if (name.endsWith(".sql")) return "#3b82f6";
  if (name.endsWith(".json")) return "#f97316";
  if (name.toLowerCase().includes("dockerfile")) return "#06b6d4";
  if (name.endsWith(".properties")) return "#8b5cf6";
  return "#64748b";
}

function TreeItem({ node, depth, selectedFile, onSelect, index = 0 }: {
  node: TreeNode; depth: number; selectedFile: string | null;
  onSelect: (p: string) => void; index?: number;
}) {
  const [open, setOpen] = useState(true);
  const isSelected = node.isFile && node.path === selectedFile;
  const color = getFileColor(node.name);

  if (node.isFile) {
    return (
      <motion.button
        initial={{ opacity: 0, x: -8 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: index * 0.025, type: "spring", stiffness: 400, damping: 30 }}
        onClick={() => onSelect(node.path)}
        whileHover={{ x: 3 }}
        whileTap={{ scale: 0.97 }}
        style={{
          width: "100%", textAlign: "left", display: "flex", alignItems: "center", gap: 7,
          padding: `5px 10px 5px ${depth * 14 + 10}px`, borderRadius: 8, border: "none",
          background: isSelected ? "linear-gradient(90deg, #eef2ff, #f5f3ff)" : "transparent",
          cursor: "pointer", fontFamily: "inherit",
          borderLeft: isSelected ? "2px solid #4f46e5" : "2px solid transparent",
          transition: "background 0.15s",
        }}
      >
        <motion.div
          animate={{ scale: isSelected ? 1.3 : 1, boxShadow: isSelected ? `0 0 6px ${color}88` : "none" }}
          style={{ width: 8, height: 8, borderRadius: "50%", background: color, flexShrink: 0 }}
        />
        <span style={{
          fontSize: 12, fontWeight: isSelected ? 600 : 400,
          color: isSelected ? "#4f46e5" : "#374151",
          overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
        }}>
          {node.name}
        </span>
      </motion.button>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, x: -8 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.025 }}
    >
      <motion.button
        onClick={() => setOpen((o) => !o)}
        whileHover={{ x: 2 }}
        style={{
          width: "100%", textAlign: "left", display: "flex", alignItems: "center", gap: 6,
          padding: `5px 10px 5px ${depth * 14 + 10}px`, borderRadius: 8, border: "none",
          background: "transparent", cursor: "pointer", fontFamily: "inherit",
        }}
      >
        <motion.div animate={{ rotate: open ? 0 : -90 }} transition={{ type: "spring", stiffness: 400 }}>
          <ChevronDown size={12} color="#94a3b8" />
        </motion.div>
        <motion.div animate={{ scale: open ? 1 : 0.9 }}>
          <Folder size={13} color="#f59e0b" fill={open ? "#fef3c7" : "none"} />
        </motion.div>
        <span style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>{node.name}</span>
      </motion.button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ type: "spring", stiffness: 400, damping: 35 }}
            style={{ overflow: "hidden" }}
          >
            {node.children.map((child, i) => (
              <TreeItem key={child.path} node={child} depth={depth + 1}
                selectedFile={selectedFile} onSelect={onSelect} index={i} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

const FEATURE_TAGS = ["Spring Boot 3.x", "Docker ready", "CI/CD included", "JWT / OAuth2"];

export const FilePreview = () => {
  const { generatedFiles, isGenerating } = useStore();
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const files = Object.keys(generatedFiles);
    if (files.length > 0 && (!selectedFile || !generatedFiles[selectedFile])) {
      setSelectedFile(files.sort()[0]);
    } else if (files.length === 0) {
      setSelectedFile(null);
    }
  }, [generatedFiles, selectedFile]);

  const handleCopy = useCallback(() => {
    if (!selectedFile) return;
    navigator.clipboard.writeText(generatedFiles[selectedFile] ?? "");
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [selectedFile, generatedFiles]);

  // Empty state
  if (Object.keys(generatedFiles).length === 0) {
    if (isGenerating) return null;
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        style={{
          height: "100%", display: "flex", flexDirection: "column",
          alignItems: "center", justifyContent: "center",
          background: "white", borderRadius: 20, border: "2px dashed #e2e8f0",
          padding: 40, textAlign: "center",
        }}
      >
        <motion.div
          animate={{ y: [0, -10, 0], rotate: [0, 3, -3, 0] }}
          transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          style={{
            width: 68, height: 68, borderRadius: 20,
            background: "linear-gradient(135deg, #eef2ff, #f5f3ff)",
            display: "flex", alignItems: "center", justifyContent: "center",
            marginBottom: 20, border: "1px solid #e0e7ff",
            boxShadow: "0 8px 24px rgba(79,70,229,0.12)",
          }}
        >
          <Terminal size={30} color="#4f46e5" />
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          style={{ fontSize: 18, fontWeight: 700, color: "#0f172a", marginBottom: 8 }}
        >
          Ready to Architect
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          style={{ fontSize: 13, color: "#94a3b8", maxWidth: 280, lineHeight: 1.6 }}
        >
          Describe your microservice on the left and hit{" "}
          <strong style={{ color: "#4f46e5" }}>Generate</strong>. Source files appear here.
        </motion.div>

        <div style={{ display: "flex", gap: 8, marginTop: 24, flexWrap: "wrap", justifyContent: "center" }}>
          {FEATURE_TAGS.map((tag, i) => (
            <motion.span
              key={tag}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 + i * 0.07, type: "spring", stiffness: 400 }}
              whileHover={{ scale: 1.05, y: -1 }}
              style={{
                padding: "4px 12px", borderRadius: 99, fontSize: 11, fontWeight: 600,
                background: "#f8fafc", border: "1px solid #e2e8f0", color: "#64748b",
                cursor: "default",
              }}
            >
              {tag}
            </motion.span>
          ))}
        </div>
      </motion.div>
    );
  }

  const fileList = Object.keys(generatedFiles).sort();
  const tree = buildTree(fileList);
  const fileColor = selectedFile ? getFileColor(selectedFile.split("/").pop() ?? "") : "#64748b";

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: "spring", stiffness: 300, damping: 28 }}
      style={{
        height: "100%", display: "flex", overflow: "hidden",
        borderRadius: 20, border: "1px solid #e2e8f0",
        background: "white", boxShadow: "0 4px 24px rgba(0,0,0,0.06)",
      }}
    >
      {/* Sidebar */}
      <div style={{ width: 240, borderRight: "1px solid #f1f5f9", display: "flex", flexDirection: "column", flexShrink: 0, background: "#fafbfc" }}>
        <div style={{ padding: "12px 14px", borderBottom: "1px solid #f1f5f9", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <span style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.06em" }}>
            Project Files
          </span>
          <motion.span
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: "spring", stiffness: 500 }}
            style={{ fontSize: 10, fontWeight: 700, padding: "2px 7px", borderRadius: 99, background: "#eef2ff", color: "#4f46e5" }}
          >
            {fileList.length}
          </motion.span>
        </div>
        <div style={{ flex: 1, overflowY: "auto", padding: "6px 4px" }}>
          {tree.map((node, i) => (
            <TreeItem key={node.path} node={node} depth={0} selectedFile={selectedFile} onSelect={setSelectedFile} index={i} />
          ))}
        </div>
      </div>

      {/* Editor */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <div style={{ height: 44, padding: "0 16px", borderBottom: "1px solid #f1f5f9", display: "flex", alignItems: "center", justifyContent: "space-between", background: "white", flexShrink: 0 }}>
          <AnimatePresence mode="wait">
            <motion.div
              key={selectedFile}
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 8 }}
              transition={{ duration: 0.15 }}
              style={{ display: "flex", alignItems: "center", gap: 8, minWidth: 0 }}
            >
              <motion.div
                animate={{ scale: [1, 1.3, 1] }}
                transition={{ duration: 0.3 }}
                style={{ width: 8, height: 8, borderRadius: "50%", background: fileColor, flexShrink: 0 }}
              />
              <FileCode size={13} color="#94a3b8" style={{ flexShrink: 0 }} />
              <span style={{ fontSize: 12, fontWeight: 500, color: "#374151", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {selectedFile ?? ""}
              </span>
            </motion.div>
          </AnimatePresence>

          <motion.button
            onClick={handleCopy}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            style={{
              display: "flex", alignItems: "center", gap: 5,
              padding: "5px 12px", borderRadius: 8, border: "1px solid #e2e8f0",
              background: copied ? "#f0fdf4" : "white", cursor: "pointer",
              fontSize: 12, fontWeight: 600, color: copied ? "#16a34a" : "#64748b",
              transition: "background 0.15s, color 0.15s", fontFamily: "inherit", flexShrink: 0,
            }}
          >
            <AnimatePresence mode="wait">
              {copied ? (
                <motion.span key="copied" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }}
                  style={{ display: "flex", alignItems: "center", gap: 5 }}>
                  <Check size={12} /> Copied!
                </motion.span>
              ) : (
                <motion.span key="copy" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }}
                  style={{ display: "flex", alignItems: "center", gap: 5 }}>
                  <Copy size={12} /> Copy
                </motion.span>
              )}
            </AnimatePresence>
          </motion.button>
        </div>

        <div style={{ flex: 1, overflow: "hidden" }}>
          <Editor
            height="100%"
            theme="vs"
            language={selectedFile ? getLanguage(selectedFile) : "plaintext"}
            path={selectedFile ?? ""}
            value={selectedFile ? generatedFiles[selectedFile] : ""}
            options={{
              readOnly: true, minimap: { enabled: false },
              fontSize: 13, fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
              padding: { top: 16, bottom: 16 }, scrollBeyondLastLine: false,
              lineNumbersMinChars: 3, renderLineHighlight: "line",
              scrollbar: { vertical: "visible", horizontal: "visible" }, wordWrap: "on",
            }}
          />
        </div>
      </div>
    </motion.div>
  );
};
