import { create } from 'zustand';

export interface GenConfig {
    // Core
    auth: string;
    database: string;
    persistence: string;
    // Messaging
    messaging: string;
    // Caching
    cache: string;
    // Build & Deploy
    buildTool: string;
    // Observability
    observability: string;
    // Architecture
    architecture: string;
    // Language
    language: string;
}

interface GenState {
    prompt: string;
    config: GenConfig;
    currentJobId: string | null;
    generatedFiles: Record<string, string>;
    isGenerating: boolean;
    jobError: string | null;
    setPrompt: (prompt: string) => void;
    setConfig: (config: Partial<GenConfig>) => void;
    setCurrentJobId: (jobId: string | null) => void;
    setGeneratedFiles: (files: Record<string, string>) => void;
    setIsGenerating: (isGenerating: boolean) => void;
    setJobError: (error: string | null) => void;
    reset: () => void;
}

const defaultConfig: GenConfig = {
    auth: 'NONE',
    database: 'NONE',
    persistence: 'NONE',
    messaging: 'NONE',
    cache: 'NONE',
    buildTool: 'MAVEN',
    observability: 'NONE',
    architecture: 'LAYERED',
    language: 'JAVA',
};

export const useStore = create<GenState>((set) => ({
    prompt: '',
    config: defaultConfig,
    currentJobId: null,
    generatedFiles: {},
    isGenerating: false,
    jobError: null,
    setPrompt: (prompt) => set({ prompt }),
    setConfig: (config) => set((state) => ({ config: { ...state.config, ...config } })),
    setCurrentJobId: (currentJobId) => set({ currentJobId }),
    setGeneratedFiles: (generatedFiles) => set({ generatedFiles }),
    setIsGenerating: (isGenerating) => set({ isGenerating }),
    setJobError: (jobError) => set({ jobError }),
    reset: () => set({
        currentJobId: null,
        generatedFiles: {},
        isGenerating: false,
        jobError: null,
    }),
}));
