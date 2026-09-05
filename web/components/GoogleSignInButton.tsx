'use client';

import { useState } from 'react';
import { createClient } from '../lib/supabase/client';

/** The one control on the sign-in page. Sends the visitor to Google and back. */
export default function GoogleSignInButton({ next }: { next: string }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function signIn() {
    setBusy(true);
    setError(null);
    const supabase = createClient();
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}`,
      },
    });
    if (error) {
      setError(error.message);
      setBusy(false);
    }
    // On success the browser is already navigating to Google; leave it busy.
  }

  return (
    <>
      <button className="primary" onClick={signIn} disabled={busy}
        style={{ display: 'inline-flex', alignItems: 'center', gap: '0.6rem' }}>
        <svg width="17" height="17" viewBox="0 0 18 18" aria-hidden focusable="false">
          <path fill="#fff" d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62Z" />
          <path fill="#fff" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.81.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18Z" opacity=".85" />
          <path fill="#fff" d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3.01-2.33Z" opacity=".7" />
          <path fill="#fff" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .96 4.95l3.01 2.33C4.68 5.16 6.66 3.58 9 3.58Z" opacity=".9" />
        </svg>
        {busy ? 'Taking you to Google…' : 'Continue with Google'}
      </button>
      {error ? (
        <p style={{ color: 'var(--caution)', fontSize: '0.88rem', marginTop: '0.7rem' }}>
          {error}
        </p>
      ) : null}
    </>
  );
}
