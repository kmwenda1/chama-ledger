"use client";
import React, { useState } from "react";
import { Sidebar } from "@/components/Sidebar";
import { useAuth } from "@/contexts/AuthContext";
import {
    CalendarDays, Sparkles, Loader2, FileText,
    CheckSquare, AlertCircle, ClipboardList,
} from "lucide-react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL + "/api/v1";

interface MeetingResult {
    decisions: any;
    actionItems: any;
    summary?: string;
}

export default function MeetingsPage() {
    const { user, getAuthHeader } = useAuth();
    const [chatText, setChatText] = useState("");
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<MeetingResult | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleProcess = async () => {
        if (!chatText.trim()) {
            setError("Please paste a WhatsApp chat export before processing.");
            return;
        }
        if (!user) {
            setError("You must be logged in to use this feature.");
            return;
        }

        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const res = await fetch(`${API_BASE}/meetings`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    ...getAuthHeader(),
                },
                body: JSON.stringify({
                    rawContent: chatText,
                    title: "Meeting " + new Date().toLocaleDateString(),
                    meetingDate: new Date().toISOString().replace("Z", ""),
                }),
            });

            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `Server error: ${res.status}`);
            }

            const data: MeetingResult = await res.json();
            setResult(data);
        } catch (err: any) {
            setError(err.message || "Failed to process meeting. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const renderList = (data: any, color: "emerald" | "blue") => {
        let items: string[] = [];

        if (!data) {
            items = [];
        } else if (Array.isArray(data)) {
            items = data.map((d: any) =>
                typeof d === "string" ? d : d.task || d.description || JSON.stringify(d)
            );
        } else if (typeof data === "string") {
            items = data.split("\n").filter(Boolean);
        } else if (typeof data === "object") {
            items = Object.values(data).map((v: any) =>
                typeof v === "string" ? v : JSON.stringify(v)
            );
        }

        const bg =
            color === "emerald"
                ? "bg-emerald-500/20 text-emerald-400"
                : "bg-blue-500/20 text-blue-400";

        if (items.length === 0) {
            return (
                <div className="text-center py-10 text-slate-500 border-2 border-dashed border-white/5 rounded-xl">
                    {color === "emerald" ? (
                        <CheckSquare size={28} className="mx-auto mb-3 opacity-30" />
                    ) : (
                        <FileText size={28} className="mx-auto mb-3 opacity-30" />
                    )}
                    Nothing found in this chat.
                </div>
            );
        }

        return (
            <ul className="space-y-3">
                {items.map((item, i) => (
                    <li
                        key={i}
                        className="flex items-start gap-3 p-3 bg-white/[0.03] rounded-xl border border-white/5"
                    >
            <span
                className={`shrink-0 w-5 h-5 rounded-full ${bg} text-[10px] font-bold flex items-center justify-center mt-0.5`}
            >
              {i + 1}
            </span>
                        <p className="text-sm text-slate-300 leading-relaxed">{item}</p>
                    </li>
                ))}
            </ul>
        );
    };

    return (
        <div className="min-h-screen bg-background text-slate-50 flex">
            <div className="fixed inset-0 pointer-events-none overflow-hidden">
                <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-primary/8 blur-[120px]" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-blue-600/5 blur-[120px]" />
            </div>

            <Sidebar />

            <main className="relative z-10 flex-1 p-6 lg:p-10 overflow-y-auto">
                <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-10 animate-fade-up">
                    <div>
                        <div className="flex items-center gap-3 mb-1">
                            <h1 className="font-display text-3xl font-extrabold tracking-tight text-white">
                                AI Meeting Minutes Parser
                            </h1>
                        </div>
                        <p className="text-slate-400 text-sm">
                            Paste a WhatsApp chat export and let AI extract decisions and action items.
                        </p>
                    </div>
                    <div className="inline-flex items-center gap-2 glass border border-purple-500/30 text-purple-400 px-5 py-3 rounded-xl font-bold text-sm">
                        <Sparkles size={16} className="animate-pulse" />
                        AI Powered
                    </div>
                </header>

                <section className="glass rounded-2xl p-8 border border-white/5 mb-6 animate-fade-up">
                    <div className="flex items-center gap-3 mb-4">
                        <ClipboardList size={18} className="text-primary" />
                        <h2 className="font-display font-bold text-lg text-white">Paste Chat Export</h2>
                    </div>
                    <p className="text-slate-400 text-xs mb-4">
                        Export your WhatsApp group chat (without media) and paste the full text below.
                    </p>
                    <textarea
                        value={chatText}
                        onChange={(e) => setChatText(e.target.value)}
                        placeholder={`[12/04/2024, 10:00 AM] John: I think we should increase contributions to KES 2000\n[12/04/2024, 10:02 AM] Mary: Agreed. Let's also follow up on Peter's loan repayment\n...`}
                        rows={12}
                        className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white text-sm placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all resize-none font-mono leading-relaxed"
                    />
                    <div className="flex items-center justify-between mt-4">
                        <p className="text-xs text-slate-500">
                            {chatText.length > 0
                                ? `${chatText.length.toLocaleString()} characters`
                                : "No text pasted yet"}
                        </p>
                        <button
                            onClick={handleProcess}
                            disabled={loading || !chatText.trim()}
                            className="flex items-center gap-2 bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed text-primary-foreground font-bold px-6 py-3 rounded-xl text-sm transition-all shadow-elegant hover:shadow-glow"
                        >
                            {loading ? (
                                <>
                                    <Loader2 size={16} className="animate-spin" />
                                    Processing...
                                </>
                            ) : (
                                <>
                                    <Sparkles size={16} />
                                    Process with AI
                                </>
                            )}
                        </button>
                    </div>
                </section>

                {error && (
                    <div className="flex items-center gap-3 text-amber-200 text-sm p-4 bg-amber-500/10 rounded-xl border border-amber-500/20 mb-6 animate-fade-up">
                        <AlertCircle size={16} className="shrink-0" />
                        {error}
                    </div>
                )}

                {result && (
                    <div className="grid gap-5 md:grid-cols-2 animate-fade-up">
                        <section className="glass rounded-2xl p-8 border border-emerald-500/20">
                            <div className="flex items-center gap-3 mb-6">
                                <div className="p-2 bg-emerald-500/10 rounded-lg">
                                    <CheckSquare size={18} className="text-emerald-400" />
                                </div>
                                <h2 className="font-display font-bold text-lg text-white">Decisions</h2>
                            </div>
                            {renderList(result.decisions, "emerald")}
                        </section>

                        <section className="glass rounded-2xl p-8 border border-blue-500/20">
                            <div className="flex items-center gap-3 mb-6">
                                <div className="p-2 bg-blue-500/10 rounded-lg">
                                    <FileText size={18} className="text-blue-400" />
                                </div>
                                <h2 className="font-display font-bold text-lg text-white">Action Items</h2>
                            </div>
                            {renderList(result.actionItems, "blue")}
                        </section>

                        {result.summary && (
                            <section className="md:col-span-2 relative group">
                                <div className="absolute -inset-px bg-gradient-to-r from-purple-500/20 to-blue-500/20 rounded-2xl blur opacity-60" />
                                <div className="relative glass rounded-2xl p-8 border border-white/10">
                                    <div className="flex items-center gap-3 mb-4">
                                        <Sparkles size={18} className="text-purple-400 animate-pulse" />
                                        <h2 className="font-display font-bold text-lg text-white">AI Summary</h2>
                                    </div>
                                    <p className="text-slate-300 text-base leading-relaxed italic">
                                        &ldquo;{result.summary}&rdquo;
                                    </p>
                                </div>
                            </section>
                        )}
                    </div>
                )}

                {!result && !loading && !error && (
                    <div className="glass rounded-2xl p-16 border border-white/5 text-center animate-fade-up">
                        <CalendarDays size={48} className="mx-auto mb-4 text-slate-600" />
                        <p className="text-slate-400 font-medium">
                            Your parsed meeting minutes will appear here.
                        </p>
                        <p className="text-slate-600 text-sm mt-1">
                            Paste a chat export above and click &ldquo;Process with AI&rdquo; to get started.
                        </p>
                    </div>
                )}
            </main>
        </div>
    );
}