"use client";
import React, { createContext, useContext, useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";

interface AuthUser {
  token: string;
  fullName: string;
  phoneNumber: string;
  role: string; // MEMBER | TREASURER | MANAGER
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (phoneNumber: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  getAuthHeader: () => Record<string, string>;
}

interface RegisterData {
  fullName: string;
  phoneNumber: string;
  email?: string;
  nationalId?: string;
  password: string;
}

const AuthContext = createContext<AuthContextType | null>(null);

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL + "/api/v1";
const STORAGE_KEY = "chamaledger_auth";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) setUser(JSON.parse(stored));
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    } finally {
      setLoading(false);
    }
  }, []);

  const login = useCallback(async (phoneNumber: string, password: string) => {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phoneNumber, password }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Login failed");
    }
    const data = await res.json();
    const authUser: AuthUser = {
      token: data.token,
      fullName: data.fullName,
      phoneNumber: data.phoneNumber,
      role: data.role || "MEMBER",
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authUser));
    setUser(authUser);
    router.push("/dashboard");
  }, [router]);

  const register = useCallback(async (formData: RegisterData) => {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(formData),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Registration failed");
    }
    const data = await res.json();
    const authUser: AuthUser = {
      token: data.token,
      fullName: data.fullName,
      phoneNumber: data.phoneNumber,
      role: data.role || "MEMBER",
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authUser));
    setUser(authUser);
    router.push("/dashboard");
  }, [router]);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
    router.push("/login");
  }, [router]);

  const getAuthHeader = useCallback((): Record<string, string> => {
    if (!user?.token) return {};
    return { Authorization: `Bearer ${user.token}` };
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, getAuthHeader }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
