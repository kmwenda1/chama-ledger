import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';

export interface DashboardData {
  personalSavings: number;
  groupBalance: number;
  activeLoansCount: number;
  aiInsight: string;
  recentTransactions: RecentTransaction[];
  role: string;
  fullName: string;
  trustScore: number;
  loanEligible: boolean;
  loanIneligibilityReason: string | null;
  pendingLoans: PendingLoan[] | null;
  mpesaLogs: MpesaLog[] | null;
}

export interface RecentTransaction {
  date: string;
  amount: number;
  type: string;
  reference: string;
  status: string;
}

export interface PendingLoan {
  id: string;
  loanNumber: string;
  amountRequested: number;
  borrowerName: string;
  borrowerPhoneNumber: string;
  purpose: string;
  status: string;
  createdAt: string;
}

export interface MpesaLog {
  id: number;
  phoneNumber: string;
  amount: number;
  status: string;
  mpesaReceiptNumber: string;
  checkoutRequestID: string;
  createdAt: string;
}

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL
    ? `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1`
    : 'http://localhost:8080/api/v1';

export const useDashboardData = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { getAuthHeader, logout } = useAuth();
  const router = useRouter();

  const fetchData = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE}/dashboard/summary`, {
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader(),
        },
      });

      if (response.status === 401 || response.status === 403) {
        logout();
        router.push('/login');
        return;
      }

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

      const result = await response.json();
      setData(result);
      setError(null);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [getAuthHeader, logout, router]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { data, loading, error, refetch: fetchData };
};