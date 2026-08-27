import type { Metadata } from 'next';
import { Archivo, Source_Sans_3 } from 'next/font/google';
import './globals.css';

const display = Archivo({ subsets: ['latin'], weight: ['500', '600', '700'], variable: '--font-display' });
const body = Source_Sans_3({ subsets: ['latin'], weight: ['400', '600'], variable: '--font-body' });

export const metadata: Metadata = {
  title: 'iCDMA',
  description:
    'Interactive construction decision-making: manage an unfolding project against weather, deliveries, and your own earlier decisions.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${display.variable} ${body.variable}`}>
      <body>{children}</body>
    </html>
  );
}
