"use client";

import { useStore } from "@/store/useStore";
import { useState } from "react";

export const PromptInput = () => {
    const { prompt, setPrompt } = useStore();
    const [localPrompt, setLocalPrompt] = useState(prompt);

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setLocalPrompt(e.target.value);
        setPrompt(e.target.value);
    };

    return (
        <textarea
            value={localPrompt}
            onChange={handleChange}
            placeholder="Describe your microservice requirements (e.g. 'Order service with PostgreSQL and Kafka')..."
            className="w-full h-[calc(100vh-180px)] bg-white border border-slate-300 text-slate-800 text-base placeholder:text-slate-400 p-5 rounded-xl focus:ring-4 focus:ring-blue-100 focus:border-blue-500 outline-none resize-none transition-all font-normal leading-relaxed shadow-sm"
        />
    );
};
