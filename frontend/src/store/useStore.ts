import { create } from 'zustand';

interface GenConfig {
    serviceType: string;
    auth: string;
    database: string;
    persistence: string;
    architecture: string;
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
}

export const useStore = create<GenState>((set) => ({
    prompt: '',
    config: {
        serviceType: 'AUTH',
        auth: 'JWT',
        database: 'MYSQL',
        persistence: 'JPA',
        architecture: 'LAYERED',
        language: 'JAVA',
    },
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
}));
