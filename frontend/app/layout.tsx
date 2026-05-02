import React from 'react';
import { Inter, Plus_Jakarta_Sans } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

// Load Inter for body text
const inter = Inter({
    subsets: ['latin'],
    variable: '--font-inter',
});

// Load Plus Jakarta Sans for headings (The Lovable Look)
const plusJakarta = Plus_Jakarta_Sans({
    subsets: ['latin'],
    variable: '--font-plus-jakarta',
});

export const metadata = {
    title: 'ChamaLedger — Secure Chama Management',
    description: 'Modern chama management: savings, loans, and contributions — all in one place.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="en" className={`dark ${inter.variable} ${plusJakarta.variable}`}>
        <body className="min-h-screen bg-background text-foreground font-sans antialiased selection:bg-primary/30 selection:text-primary-foreground">
        <div className="fixed inset-0 z-[-1] opacity-[0.03] pointer-events-none bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" />
        <Providers>
          {children}
        </Providers>
        </body>
        </html>
    );
}