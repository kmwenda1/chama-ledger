"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import {
  Wallet, LayoutDashboard, History, TrendingUp, Users,
  ArrowLeft, LogOut, CalendarDays,
} from "lucide-react";

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

const NAV_ITEMS = [
  { label: "Dashboard", icon: LayoutDashboard, href: "/dashboard" },
  { label: "Meetings", icon: CalendarDays, href: "/meetings" },
  { label: "Activity", icon: History, href: null },
  { label: "Loans", icon: TrendingUp, href: null },
  { label: "Members", icon: Users, href: null },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const role = user?.role || "MEMBER";
  const displayName = user?.fullName || "Member";

  return (
    <aside className="relative z-10 w-64 glass-strong p-6 hidden md:flex flex-col gap-8">
      <Link
        href="/"
        className="flex items-center gap-3 font-display font-bold text-xl tracking-tight text-white hover:opacity-80 transition-opacity"
      >
        <div className="bg-primary p-2 rounded-xl shadow-glow-sm">
          <Wallet size={18} className="text-primary-foreground" />
        </div>
        ChamaLedger
      </Link>

      <nav className="space-y-1">
        {NAV_ITEMS.map(({ label, icon: Icon, href }) => {
          const isActive = href !== null && pathname === href;
          const baseClass = "flex items-center gap-3 px-4 py-3 rounded-xl transition-all";

          if (href === null) {
            return (
              <div
                key={label}
                className={`${baseClass} text-slate-400 hover:bg-white/5 hover:text-white cursor-pointer`}
              >
                <Icon size={17} />
                <span className="font-medium text-sm">{label}</span>
              </div>
            );
          }

          return (
            <Link
              key={label}
              href={href}
              className={`${baseClass} ${
                isActive
                  ? "bg-primary/10 text-primary border border-primary/20"
                  : "text-slate-400 hover:bg-white/5 hover:text-white"
              }`}
            >
              <Icon size={17} />
              <span className={isActive ? "font-semibold text-sm" : "font-medium text-sm"}>
                {label}
              </span>
            </Link>
          );
        })}
      </nav>

      <div className="mt-auto space-y-3">
        {user && (
          <div className="px-4 py-3 glass rounded-xl border border-white/5">
            <p className="text-xs font-bold text-white truncate">{displayName}</p>
            <p className="text-[10px] text-slate-500 truncate">{user.phoneNumber}</p>
            <span
              className={`inline-block mt-1.5 text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full border ${
                roleBadgeColor[role] || roleBadgeColor.MEMBER
              }`}
            >
              {roleLabel[role] || role}
            </span>
          </div>
        )}
        <button
          onClick={logout}
          className="w-full flex items-center gap-2 text-xs text-slate-500 hover:text-white transition-colors px-4 py-2 rounded-lg hover:bg-white/5"
        >
          <LogOut size={14} /> Sign Out
        </button>
        <Link
          href="/"
          className="flex items-center gap-2 text-xs text-slate-500 hover:text-white transition-colors px-4 py-2"
        >
          <ArrowLeft size={14} /> Back to Home
        </Link>
      </div>
    </aside>
  );
}
