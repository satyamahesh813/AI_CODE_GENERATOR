"use client";

import { motion, AnimatePresence } from "framer-motion";
import { AlertTriangle, X, RefreshCw, Wifi, Clock, Key, Shield } from "lucide-react";

interface ErrorToastProps {
    error: string | null;
    onDismiss: () => void;
    onRetry?: () => void;
}

function getErrorMeta(error: string): {
    icon: React.ReactNode;
    title: string;
    message: string;
    suggestion: string;
    color: string;
    bg: string;
    border: string;
} {
    const e = error.toLowerCase();

    if (e.includes("rate limit") || e.includes("quota") || e.includes("429") || e.includes("too many requests")) {
        return {
            icon: <Clock size={18} color="#d97706" />,
            title: "AI is taking a breather",
            message: "The AI service is temporarily rate-limited due to high usage.",
            suggestion: "Wait a moment and try again — it usually recovers in under a minute.",
            color: "#92400e",
            bg: "#fffbeb",
            border: "#fde68a",
        };
    }

    if (e.includes("daily quota") || e.includes("limit: 0") || e.includes("exhausted")) {
        return {
            icon: <Clock size={18} color="#d97706" />,
            title: "Daily limit reached",
            message: "The free AI quota for today has been used up.",
            suggestion: "The quota resets at midnight. You can also switch to a different AI model in application.properties.",
            color: "#92400e",
            bg: "#fffbeb",
            border: "#fde68a",
        };
    }

    if (e.includes("api key") || e.includes("missing") || e.includes("invalid key") || e.includes("401")) {
        return {
            icon: <Key size={18} color="#dc2626" />,
            title: "API key not configured",
            message: "The AI service key is missing or invalid.",
            suggestion: "Add your API key to backend/src/main/resources/application.properties and restart the backend.",
            color: "#991b1b",
            bg: "#fef2f2",
            border: "#fecaca",
        };
    }

    if (e.includes("not found") || e.includes("404") || e.includes("model")) {
        return {
            icon: <AlertTriangle size={18} color="#dc2626" />,
            title: "AI model not available",
            message: "The selected AI model couldn't be reached.",
            suggestion: "Try changing gemini.model to gemini-2.0-flash-lite in application.properties.",
            color: "#991b1b",
            bg: "#fef2f2",
            border: "#fecaca",
        };
    }

    if (e.includes("network") || e.includes("connect") || e.includes("timeout") || e.includes("communicate")) {
        return {
            icon: <Wifi size={18} color="#dc2626" />,
            title: "Connection issue",
            message: "Couldn't reach the AI service.",
            suggestion: "Check your internet connection and make sure the backend is running on port 8081.",
            color: "#991b1b",
            bg: "#fef2f2",
            border: "#fecaca",
        };
    }

    if (e.includes("governance") || e.includes("secret")) {
        return {
            icon: <Shield size={18} color="#7c3aed" />,
            title: "Security check failed",
            message: "The generated code may contain sensitive data.",
            suggestion: "Try rephrasing your prompt to avoid including credentials or secrets.",
            color: "#5b21b6",
            bg: "#f5f3ff",
            border: "#ddd6fe",
        };
    }

    // Generic fallback
    return {
        icon: <AlertTriangle size={18} color="#dc2626" />,
        title: "Generation failed",
        message: "Something went wrong while generating your microservice.",
        suggestion: "Try rephrasing your prompt or simplifying the requirements.",
        color: "#991b1b",
        bg: "#fef2f2",
        border: "#fecaca",
    };
}

export const ErrorToast = ({ error, onDismiss, onRetry }: ErrorToastProps) => {
    if (!error) return null;

    const meta = getErrorMeta(error);

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0, y: -20, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -20, scale: 0.96 }}
                transition={{ type: "spring", stiffness: 400, damping: 30 }}
                style={{
                    position: "fixed",
                    top: 72,
                    left: "50%",
                    transform: "translateX(-50%)",
                    zIndex: 10000,
                    width: "min(520px, calc(100vw - 32px))",
                    background: meta.bg,
                    border: `1.5px solid ${meta.border}`,
                    borderRadius: 16,
                    boxShadow: "0 8px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06)",
                    padding: "16px 18px",
                    display: "flex",
                    gap: 14,
                    alignItems: "flex-start",
                }}
            >
                {/* Icon */}
                <div style={{
                    width: 36, height: 36, borderRadius: 10,
                    background: "white",
                    border: `1px solid ${meta.border}`,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    flexShrink: 0,
                    boxShadow: "0 1px 4px rgba(0,0,0,0.06)",
                }}>
                    {meta.icon}
                </div>

                {/* Content */}
                <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 700, color: meta.color, marginBottom: 3 }}>
                        {meta.title}
                    </div>
                    <div style={{ fontSize: 13, color: meta.color, opacity: 0.85, lineHeight: 1.5, marginBottom: 6 }}>
                        {meta.message}
                    </div>
                    <div style={{
                        fontSize: 12, color: meta.color, opacity: 0.7,
                        lineHeight: 1.5, padding: "6px 10px",
                        background: "rgba(255,255,255,0.6)", borderRadius: 8,
                        border: `1px solid ${meta.border}`,
                    }}>
                        💡 {meta.suggestion}
                    </div>

                    {onRetry && (
                        <button
                            onClick={onRetry}
                            style={{
                                marginTop: 10, display: "flex", alignItems: "center", gap: 5,
                                padding: "5px 12px", borderRadius: 8, border: `1px solid ${meta.border}`,
                                background: "white", color: meta.color, fontSize: 12, fontWeight: 600,
                                cursor: "pointer", fontFamily: "inherit",
                            }}
                        >
                            <RefreshCw size={12} /> Try Again
                        </button>
                    )}
                </div>

                {/* Dismiss */}
                <button
                    onClick={onDismiss}
                    style={{
                        width: 28, height: 28, borderRadius: 8, border: `1px solid ${meta.border}`,
                        background: "white", display: "flex", alignItems: "center", justifyContent: "center",
                        cursor: "pointer", flexShrink: 0, color: meta.color, opacity: 0.6,
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.opacity = "1"; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.opacity = "0.6"; }}
                >
                    <X size={14} />
                </button>
            </motion.div>
        </AnimatePresence>
    );
};
