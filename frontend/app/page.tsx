"use client";
import React from "react";
import Link from "next/link";
import { ArrowRight, Shield, TrendingUp, Users, Sparkles, Wallet, ChevronRight } from "lucide-react";

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-gradient-hero overflow-hidden">
      {/* Ambient glow orbs */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden" aria-hidden>
        <div className="absolute top-[-10%] left-[20%] w-[500px] h-[500px] rounded-full bg-primary/10 blur-[120px] animate-pulse-glow" />
        <div className="absolute bottom-[-10%] right-[10%] w-[400px] h-[400px] rounded-full bg-primary/8 blur-[100px] animate-pulse-glow [animation-delay:2s]" />
      </div>

      {/* Nav */}
      <nav className="relative z-10 flex items-center justify-between px-6 py-5 max-w-7xl mx-auto">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-primary flex items-center justify-center shadow-glow-sm">
            <Wallet size={16} className="text-white" />
          </div>
          <span className="font-display font-bold text-lg tracking-tight text-white">ChamaLedger</span>
        </div>
        <div className="flex items-center gap-3">
          <Link
            href="/login"
            className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors px-4 py-2"
          >
            Sign In
          </Link>
          <Link
            href="/register"
            className="text-sm font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-5 py-2.5 rounded-xl transition-all shadow-elegant hover:shadow-glow"
          >
            Get Started
          </Link>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative z-10 flex flex-col items-center text-center px-6 pt-20 pb-24 max-w-5xl mx-auto">
        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass border-gradient text-xs font-semibold text-primary mb-8 animate-fade-up">
          <Sparkles size={12} />
          Powered by M-Pesa & AI Insights
        </div>

        <h1
          className="font-display font-extrabold text-5xl md:text-7xl leading-[1.08] tracking-tight text-white mb-6 animate-fade-up [animation-delay:0.1s] opacity-0"
          style={{ animationFillMode: "forwards" }}
        >
          Your Chama,{" "}
          <span className="bg-gradient-to-r from-primary via-blue-400 to-primary bg-clip-text text-transparent glow-text">
            Reimagined
          </span>
        </h1>

        <p
          className="text-lg md:text-xl text-muted-foreground max-w-2xl leading-relaxed mb-10 animate-fade-up [animation-delay:0.2s] opacity-0"
          style={{ animationFillMode: "forwards" }}
        >
          Track contributions, manage loans, and grow your group savings — with real-time M-Pesa
          integration and AI-powered insights built for Kenyan chamas.
        </p>

        <div
          className="flex flex-col sm:flex-row gap-4 animate-fade-up [animation-delay:0.3s] opacity-0"
          style={{ animationFillMode: "forwards" }}
        >
          <Link
            href="/login"
            className="group inline-flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground font-bold px-8 py-4 rounded-xl text-base transition-all shadow-elegant hover:shadow-glow"
          >
            Open Dashboard
            <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
          </Link>
          <a
            href="#features"
            className="inline-flex items-center gap-2 glass hover:bg-white/5 text-foreground font-semibold px-8 py-4 rounded-xl text-base transition-all"
          >
            See Features <ChevronRight size={18} />
          </a>
        </div>

        {/* Floating stat cards */}
        <div className="relative mt-20 w-full max-w-3xl animate-fade-up [animation-delay:0.5s] opacity-0" style={{ animationFillMode: "forwards" }}>
          <div className="absolute inset-0 bg-gradient-to-r from-primary/20 via-transparent to-primary/20 rounded-3xl blur-3xl" />
          <div className="relative grid grid-cols-3 gap-4 glass-strong rounded-3xl p-6">
            {[
              { label: "Active Chamas", value: "2,400+", icon: <Users size={20} className="text-blue-400" /> },
              { label: "KES Managed", value: "142M+", icon: <TrendingUp size={20} className="text-emerald-400" /> },
              { label: "Transactions/mo", value: "18K+", icon: <Wallet size={20} className="text-amber-400" /> },
            ].map((stat) => (
              <div key={stat.label} className="flex flex-col items-center gap-2 py-4">
                <div className="p-2.5 bg-white/5 rounded-xl">{stat.icon}</div>
                <span className="font-display font-black text-2xl text-white">{stat.value}</span>
                <span className="text-xs text-muted-foreground font-medium">{stat.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="relative z-10 px-6 py-24 max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="font-display font-bold text-4xl text-white mb-4">Everything your chama needs</h2>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto">
            From first contribution to loan disbursement — fully automated.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {[
            {
              icon: <Wallet size={24} className="text-blue-400" />,
              title: "M-Pesa Integration",
              desc: "Instant STK push for contributions. Real-time settlement via Daraja API.",
            },
            {
              icon: <TrendingUp size={24} className="text-emerald-400" />,
              title: "Loan Management",
              desc: "Apply, review, and disburse loans with automated repayment tracking.",
            },
            {
              icon: <Sparkles size={24} className="text-purple-400" />,
              title: "AI Insights",
              desc: "Gemini-powered analysis on your group health, spending patterns, and growth.",
            },
            {
              icon: <Shield size={24} className="text-amber-400" />,
              title: "Secure Auth",
              desc: "OTP-verified registration. JWT-protected endpoints. Bank-grade security.",
            },
            {
              icon: <Users size={24} className="text-pink-400" />,
              title: "Member Roles",
              desc: "Chairperson, treasurer, and member roles with granular permissions.",
            },
            {
              icon: <ArrowRight size={24} className="text-cyan-400" />,
              title: "Meeting Records",
              desc: "Upload minutes, track attendance, and store decisions per meeting.",
            },
          ].map((f) => (
            <div
              key={f.title}
              className="group glass hover:glass-strong rounded-2xl p-7 transition-all hover:shadow-elegant hover:-translate-y-1 animate-float"
              style={{ animationDelay: `${Math.random() * 2}s` }}
            >
              <div className="p-3 bg-white/5 rounded-xl w-fit mb-4 group-hover:bg-white/10 transition-colors">
                {f.icon}
              </div>
              <h3 className="font-display font-bold text-white mb-2">{f.title}</h3>
              <p className="text-muted-foreground text-sm leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="relative z-10 px-6 py-20 max-w-3xl mx-auto text-center">
        <div className="relative glass-strong rounded-3xl p-12 border-gradient overflow-hidden">
          <div className="shimmer absolute inset-0 rounded-3xl" />
          <h2 className="relative font-display font-extrabold text-3xl md:text-4xl text-white mb-4">
            Ready to modernize your chama?
          </h2>
          <p className="relative text-muted-foreground mb-8">
            Join thousands of Kenyan groups already managing funds with ChamaLedger.
          </p>
          <Link
            href="/register"
            className="relative inline-flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground font-bold px-8 py-4 rounded-xl transition-all shadow-elegant hover:shadow-glow"
          >
            Start for Free <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="relative z-10 border-t border-border/30 px-6 py-8 text-center text-sm text-muted-foreground">
        © {new Date().getFullYear()} ChamaLedger. Built for Kenyan chamas.
      </footer>
    </div>
  );
}
