"use client";
import React, { useState } from "react";
import Link from "next/link";
import { useDashboardData, PendingLoan, MpesaLog } from "@/hooks/useDashboardData";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/Toast";
import { Sidebar } from "@/components/Sidebar";
import {
  Wallet, Users, PiggyBank, TrendingUp,
  AlertCircle, Sparkles, Plus, CheckCircle, XCircle,
  Shield, Clock, Banknote, Loader2, Lock,
} from "lucide-react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL + "/api/v1";

function TrustScoreGauge({ score }: { score: number }) {
  const radius = 42;
  const circumference = 2 * Math.PI * radius;
  const progress = (score / 100) * circumference;
  const color = score >= 60 ? "#34d399" : score >= 40 ? "#fbbf24" : "#f87171";

  return (
    <div className="flex flex-col items-center gap-2">
      <svg width="110" height="110" viewBox="0 0 110 110">
        <circle cx="55" cy="55" r={radius} stroke="rgba(255,255,255,0.06)" strokeWidth="10" fill="none" />
        <circle
          cx="55" cy="55" r={radius}
          stroke={color}
          strokeWidth="10"
          fill="none"
          strokeDasharray={`${progress} ${circumference}`}
          strokeLinecap="round"
          transform="rotate(-90 55 55)"
          style={{ transition: "stroke-dasharray 0.8s ease" }}
        />
        <text x="55" y="55" textAnchor="middle" dominantBaseline="central" fill="white" fontSize="20" fontWeight="800" fontFamily="Plus Jakarta Sans, sans-serif">
          {score}
        </text>
        <text x="55" y="72" textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="9" fontFamily="Plus Jakarta Sans, sans-serif">
          /100
        </text>
      </svg>
      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Trust Score</span>
    </div>
  );
}

function StatCard({ title, value, icon, color, trend }: { title: string; value: string; icon: React.ReactNode; color: string; trend: string }) {
  const borderColors: Record<string, string> = {
    blue: "border-blue-500/20 hover:border-blue-500/40",
    emerald: "border-emerald-500/20 hover:border-emerald-500/40",
    amber: "border-amber-500/20 hover:border-amber-500/40",
  };
  return (
    <div className={`relative overflow-hidden glass rounded-2xl p-7 transition-all hover:-translate-y-1 border ${borderColors[color]}`}>
      <div className="absolute inset-0 translate-x-[-100%] bg-gradient-to-r from-transparent via-white/[0.03] to-transparent animate-shimmer pointer-events-none" />
      <div className="flex items-center justify-between mb-4">
        <span className="text-xs font-bold text-slate-500 uppercase tracking-tighter">{title}</span>
        <div className="p-2 bg-white/5 rounded-lg">{icon}</div>
      </div>
      <div className="text-2xl font-black font-display text-white tracking-tight mb-1">{value}</div>
      <p className="text-[10px] font-medium text-slate-400">{trend}</p>
    </div>
  );
}

function MemberView({ data, onContribute, contributing }: {
  data: NonNullable<ReturnType<typeof useDashboardData>["data"]>;
  onContribute: () => void;
  contributing: boolean;
}) {
  return (
    <>
      <div className="grid gap-5 md:grid-cols-3 mb-8">
        <StatCard
          title="Personal Savings"
          value={`KES ${Number(data.personalSavings).toLocaleString()}`}
          icon={<PiggyBank size={20} className="text-emerald-400" />}
          color="emerald"
          trend="Total contributions"
        />
        <StatCard
          title="Group Balance"
          value={`KES ${Number(data.groupBalance).toLocaleString()}`}
          icon={<Users size={20} className="text-blue-400" />}
          color="blue"
          trend="Pool funds"
        />
        <StatCard
          title="Active Loans"
          value={String(data.activeLoansCount)}
          icon={<Wallet size={20} className="text-amber-400" />}
          color="amber"
          trend={data.activeLoansCount ? "Repayment in progress" : "No active loans"}
        />
      </div>

      <div className="grid gap-5 md:grid-cols-2 mb-8">
        <div className="glass rounded-2xl p-7 border border-white/5 flex flex-col items-center justify-center gap-4">
          <TrustScoreGauge score={data.trustScore} />
          {data.loanEligible ? (
            <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
              <CheckCircle size={14} /> Eligible for a loan
            </div>
          ) : (
            <div className="text-center">
              <div className="flex items-center gap-2 text-red-400 text-xs font-semibold mb-1 justify-center">
                <XCircle size={14} /> Not eligible yet
              </div>
              {data.loanIneligibilityReason && (
                <p className="text-slate-500 text-[11px] leading-snug">{data.loanIneligibilityReason}</p>
              )}
            </div>
          )}
        </div>

        <div className="glass rounded-2xl p-7 border border-white/5 flex flex-col gap-4">
          <h3 className="font-display font-bold text-white text-sm">Quick Actions</h3>

          <button
            onClick={onContribute}
            disabled={contributing}
            className="flex items-center justify-center gap-2 bg-primary hover:bg-primary/90 disabled:opacity-60 disabled:cursor-not-allowed text-primary-foreground font-bold px-5 py-3 rounded-xl text-sm transition-all shadow-elegant hover:shadow-glow"
          >
            {contributing ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />}
            {contributing ? "Sending prompt…" : "Contribute via M-Pesa"}
          </button>

          {data.loanEligible ? (
            <Link
              href="/loans/apply"
              className="flex items-center justify-center gap-2 glass border border-primary/30 text-primary font-bold px-5 py-3 rounded-xl text-sm transition-all hover:bg-primary/10"
            >
              <Banknote size={16} /> Request Loan
            </Link>
          ) : (
            <div className="relative group">
              <button
                disabled
                className="w-full flex items-center justify-center gap-2 glass border border-white/10 text-slate-500 font-bold px-5 py-3 rounded-xl text-sm cursor-not-allowed"
              >
                <Lock size={16} /> Request Loan
              </button>
              <div className="absolute bottom-full left-0 right-0 mb-2 p-3 glass-strong rounded-xl text-xs text-slate-300 border border-white/10 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                {data.loanIneligibilityReason || "Not eligible at this time."}
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

function ManagerView({ pendingLoans }: { pendingLoans: PendingLoan[] | null }) {
  return (
    <section className="glass rounded-2xl p-8 border border-white/5 mb-8">
      <div className="flex items-center gap-3 mb-6">
        <Shield size={18} className="text-purple-400" />
        <h2 className="font-display font-bold text-lg text-white">Loan Approval Queue</h2>
        {pendingLoans?.length ? (
          <span className="ml-auto text-xs font-bold bg-amber-500/20 text-amber-400 px-2.5 py-1 rounded-full">
            {pendingLoans.length} pending
          </span>
        ) : null}
      </div>
      {pendingLoans?.length ? (
        <div className="space-y-4">
          {pendingLoans.map((loan) => (
            <div key={loan.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 bg-white/[0.03] rounded-xl border border-white/5">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-bold text-primary">{loan.loanNumber}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-400 font-semibold">PENDING</span>
                </div>
                <p className="text-sm font-semibold text-white">{loan.borrowerName}</p>
                <p className="text-xs text-slate-500">{loan.borrowerPhoneNumber}</p>
                {loan.purpose && <p className="text-xs text-slate-400 mt-1 italic">"{loan.purpose}"</p>}
              </div>
              <div className="text-right">
                <p className="text-lg font-black text-white font-display">
                  KES {Number(loan.amountRequested).toLocaleString()}
                </p>
                <div className="flex gap-2 mt-2 justify-end">
                  <button className="text-xs font-bold bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1">
                    <CheckCircle size={12} /> Approve
                  </button>
                  <button className="text-xs font-bold bg-red-500/20 hover:bg-red-500/30 text-red-400 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1">
                    <XCircle size={12} /> Reject
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-10 text-slate-500 border-2 border-dashed border-white/5 rounded-xl">
          <Clock size={32} className="mx-auto mb-3 opacity-30" />
          No pending loan applications.
        </div>
      )}
    </section>
  );
}

function TreasurerView({ mpesaLogs }: { mpesaLogs: MpesaLog[] | null }) {
  return (
    <section className="glass rounded-2xl p-8 border border-white/5 mb-8">
      <div className="flex items-center gap-3 mb-6">
        <TrendingUp size={18} className="text-blue-400" />
        <h2 className="font-display font-bold text-lg text-white">M-Pesa Transaction Logs</h2>
        <span className="ml-auto text-xs font-bold bg-blue-500/20 text-blue-400 px-2.5 py-1 rounded-full">
          Last 20
        </span>
      </div>
      {mpesaLogs?.length ? (
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-left text-slate-500 border-b border-white/5">
                <th className="pb-3 font-semibold">Phone</th>
                <th className="pb-3 font-semibold">Amount</th>
                <th className="pb-3 font-semibold">Receipt</th>
                <th className="pb-3 font-semibold">Status</th>
                <th className="pb-3 font-semibold">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {mpesaLogs.map((log) => (
                <tr key={log.id} className="hover:bg-white/[0.03] transition-colors">
                  <td className="py-3 text-slate-300">{log.phoneNumber}</td>
                  <td className="py-3 font-bold text-white">KES {log.amount?.toLocaleString()}</td>
                  <td className="py-3 text-slate-400 font-mono">{log.mpesaReceiptNumber || log.checkoutRequestID?.slice(0, 12) + "…"}</td>
                  <td className="py-3">
                    <span className={`px-2 py-0.5 rounded-full font-bold ${
                      log.status === "SUCCESS"
                        ? "bg-emerald-500/20 text-emerald-400"
                        : log.status === "FAILED"
                        ? "bg-red-500/20 text-red-400"
                        : "bg-amber-500/20 text-amber-400"
                    }`}>
                      {log.status}
                    </span>
                  </td>
                  <td className="py-3 text-slate-500">
                    {log.createdAt ? new Date(log.createdAt).toLocaleDateString() : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-center py-10 text-slate-500 border-2 border-dashed border-white/5 rounded-xl">
          No M-Pesa transactions recorded yet.
        </div>
      )}
    </section>
  );
}

export default function Dashboard() {
  const { data, loading, error, refetch } = useDashboardData();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [contributing, setContributing] = useState(false);
  const [contributeAmount, setContributeAmount] = useState("500");
  const [showContributeModal, setShowContributeModal] = useState(false);

  const handleContribute = async () => {
    if (!user) return;
    setContributing(true);
    try {
      const res = await fetch(`${API_BASE}/payments/stk-push`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({ amount: contributeAmount, reference: "CHAMA-CONTRIBUTION" }),
      });
      if (!res.ok) throw new Error("STK Push request failed");
      showToast(`M-Pesa prompt sent to ${user.phoneNumber}. Enter your PIN to confirm.`, "success");
      setShowContributeModal(false);

      let attempts = 0;
      const prevSavings = data?.personalSavings;
      const poll = setInterval(async () => {
        attempts++;
        await refetch();
        if (attempts >= 24) {
          clearInterval(poll);
          return;
        }
        if (data && data.personalSavings !== prevSavings) {
          showToast("Payment confirmed! Your savings have been updated.", "success");
          clearInterval(poll);
        }
      }, 5000);
    } catch (err: any) {
      showToast(err.message || "Failed to initiate payment.", "error");
    } finally {
      setContributing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-full border-2 border-primary/20 border-t-primary animate-spin" />
          <p className="text-slate-400 text-sm font-medium animate-pulse">Syncing Ledger…</p>
        </div>
      </div>
    );
  }

  const role = data?.role || user?.role || "MEMBER";
  const displayName = data?.fullName || user?.fullName || "Member";

  const roleLabel: Record<string, string> = {
    MANAGER: "Manager",
    TREASURER: "Treasurer",
    MEMBER: "Member",
  };

  const roleBadgeColor: Record<string, string> = {
    MANAGER: "bg-purple-500/20 text-purple-400 border-purple-500/30",
    TREASURER: "bg-blue-500/20 text-blue-400 border-blue-500/30",
    MEMBER: "bg-primary/10 text-primary border-primary/20",
  };

  return (
    <>
      {showContributeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="glass-strong rounded-3xl p-8 border border-white/10 shadow-elegant w-full max-w-sm mx-4 animate-fade-up">
            <h2 className="font-display font-bold text-xl text-white mb-2">Contribute via M-Pesa</h2>
            <p className="text-slate-400 text-sm mb-6">
              A payment prompt will be sent to <span className="text-white font-semibold">{user?.phoneNumber}</span>.
            </p>
            <div className="space-y-4">
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Amount (KES)</label>
                <input
                  type="number"
                  value={contributeAmount}
                  onChange={(e) => setContributeAmount(e.target.value)}
                  min="10"
                  step="50"
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
                />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => setShowContributeModal(false)}
                  className="flex-1 py-3 rounded-xl glass border border-white/10 text-slate-300 text-sm font-semibold hover:bg-white/5 transition-all"
                >
                  Cancel
                </button>
                <button
                  onClick={handleContribute}
                  disabled={contributing}
                  className="flex-1 py-3 rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-bold transition-all shadow-elegant flex items-center justify-center gap-2"
                >
                  {contributing ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
                  {contributing ? "Sending…" : "Send Prompt"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="min-h-screen bg-background text-slate-50 flex">
        <div className="fixed inset-0 pointer-events-none overflow-hidden">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-primary/8 blur-[120px]" />
          <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-blue-600/5 blur-[120px]" />
        </div>

        <Sidebar />

        <main className="relative z-10 flex-1 p-6 lg:p-10 overflow-y-auto">
          <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-10">
            <div className="animate-fade-up">
              <div className="flex items-center gap-3 mb-1">
                <h1 className="font-display text-3xl font-extrabold tracking-tight text-white">
                  {roleLabel[role] || "Member"} Dashboard
                </h1>
                <span className={`text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full border ${roleBadgeColor[role] || roleBadgeColor.MEMBER}`}>
                  {roleLabel[role] || role}
                </span>
              </div>
              <p className="text-slate-400 text-sm">
                Welcome back, <span className="text-white font-semibold">{displayName}</span>
              </p>
            </div>
            {role === "MEMBER" && (
              <button
                onClick={() => setShowContributeModal(true)}
                className="group inline-flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground px-6 py-3 rounded-xl font-bold transition-all shadow-elegant hover:shadow-glow text-sm"
              >
                <Plus size={16} className="group-hover:rotate-90 transition-transform duration-200" />
                Contribute
              </button>
            )}
            {role === "TREASURER" && (
              <div className="inline-flex items-center gap-2 glass border border-blue-500/30 text-blue-400 px-5 py-3 rounded-xl font-bold text-sm">
                <TrendingUp size={16} />
                Finance Overview
              </div>
            )}
            {role === "MANAGER" && (
              <div className="inline-flex items-center gap-2 glass border border-purple-500/30 text-purple-400 px-5 py-3 rounded-xl font-bold text-sm">
                <Shield size={16} />
                Approvals
              </div>
            )}
          </header>

          <div className="grid gap-5 md:grid-cols-3 mb-8">
            <StatCard
              title="Group Balance"
              value={`KES ${Number(data?.groupBalance ?? 0).toLocaleString()}`}
              icon={<Users size={20} className="text-blue-400" />}
              color="blue"
              trend="Total pool funds"
            />
            <StatCard
              title="Personal Savings"
              value={`KES ${Number(data?.personalSavings ?? 0).toLocaleString()}`}
              icon={<PiggyBank size={20} className="text-emerald-400" />}
              color="emerald"
              trend="Your contributions"
            />
            <StatCard
              title="Active Loans"
              value={String(data?.activeLoansCount ?? 0)}
              icon={<Wallet size={20} className="text-amber-400" />}
              color="amber"
              trend={data?.activeLoansCount ? "In progress" : "No active loans"}
            />
          </div>

          {role === "MEMBER" && data && (
            <MemberView
              data={data}
              onContribute={() => setShowContributeModal(true)}
              contributing={contributing}
            />
          )}

          {role === "MANAGER" && (
            <ManagerView pendingLoans={data?.pendingLoans ?? null} />
          )}

          {role === "TREASURER" && (
            <TreasurerView mpesaLogs={data?.mpesaLogs ?? null} />
          )}

          <div className="relative group mb-8 animate-fade-up">
            <div className="absolute -inset-px bg-gradient-to-r from-purple-500/20 to-blue-500/20 rounded-2xl blur opacity-60" />
            <section className="relative glass rounded-2xl p-8 border border-white/10">
              <div className="flex items-center gap-3 mb-4">
                <Sparkles size={20} className="text-purple-400 animate-pulse-glow" />
                <h2 className="font-display font-bold text-lg text-white">Gemini Insights</h2>
              </div>
              <p className="text-slate-300 text-base leading-relaxed italic">
                &ldquo;{data?.aiInsight ?? "Analyzing your chama's financial health…"}&rdquo;
              </p>
            </section>
          </div>

          <section className="glass rounded-2xl p-8 border border-white/5 animate-fade-up">
            <h2 className="font-display font-bold text-lg text-white mb-6">Recent Activity</h2>
            {data?.recentTransactions?.length ? (
              <div className="space-y-3">
                {data.recentTransactions.map((tx, i) => (
                  <div key={i} className="flex items-center justify-between py-3 border-b border-white/5 last:border-0 hover:bg-white/[0.03] px-2 rounded-lg transition-colors">
                    <div>
                      <p className="font-medium text-sm text-white">{tx.reference || tx.type}</p>
                      <p className="text-xs text-slate-500">{tx.date ? new Date(tx.date).toLocaleDateString() : ""}</p>
                    </div>
                    <span className={`font-bold text-sm ${tx.type === "CREDIT" ? "text-emerald-400" : "text-red-400"}`}>
                      {tx.type === "CREDIT" ? "+" : "-"}KES {Number(tx.amount).toLocaleString()}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-10 text-slate-500 border-2 border-dashed border-white/5 rounded-xl">
                No transactions recorded yet.
              </div>
            )}
          </section>

          {error && (
            <div className="mt-6 flex items-center gap-3 text-amber-200 text-sm p-4 bg-amber-500/10 rounded-xl border border-amber-500/20">
              <AlertCircle size={16} className="shrink-0" />
              {error}
            </div>
          )}
        </main>
      </div>
    </>
  );
}
