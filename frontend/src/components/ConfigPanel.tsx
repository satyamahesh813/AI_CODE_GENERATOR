"use client";

import { useStore } from "@/store/useStore";

export const ConfigPanel = () => {
    const { config, setConfig } = useStore();

    const sections = [
        {
            key: "serviceType",
            label: "Service Type",
            vals: ["AUTH", "INVENTORY", "ORDER", "PAYMENT", "GATEWAY"]
        },
        {
            key: "auth",
            label: "Security Strategy",
            vals: ["JWT", "OAUTH2", "NONE"]
        },
        {
            key: "database",
            label: "Database Engine",
            vals: ["MYSQL", "POSTGRESQL", "H2", "NONE"]
        },
        {
            key: "persistence",
            label: "Persistence Layer",
            vals: ["JPA", "MYBATIS", "NONE"]
        },
    ];

    return (
        <div className="flex flex-col gap-8">
            {sections.map((section) => (
                <div key={section.key} className="flex flex-col gap-3">
                    <div className="flex flex-wrap gap-3">
                        {section.vals.map((v) => {
                            const isSelected = (config as any)[section.key] === v;
                            return (
                                <button
                                    key={v}
                                    onClick={() => setConfig({ [section.key]: v })}
                                    className={`px-5 py-3 rounded-lg text-sm font-semibold transition-all border flex items-center justify-center min-w-[80px] ${isSelected
                                        ? "bg-blue-600 border-blue-600 text-white shadow-md ring-2 ring-blue-100"
                                        : "bg-white border-slate-200 text-slate-600 hover:border-slate-300 hover:bg-slate-50"
                                        }`}
                                >
                                    {v}
                                </button>
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
};
