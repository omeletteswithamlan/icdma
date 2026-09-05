import Link from 'next/link';
import { redirect } from 'next/navigation';
import GoogleSignInButton from '../../components/GoogleSignInButton';
import { createClient } from '../../lib/supabase/server';
import { safeNext } from '../../lib/safeNext';

export const metadata = {
  title: 'Sign in — iCDMA',
  description: 'Sign in with Google to open the Fundamentals of Construction modules.',
};

export default async function SignInPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string; error?: string }>;
}) {
  const { next, error } = await searchParams;
  const target = safeNext(next);

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (user) redirect(target);

  return (
    <main style={{ maxWidth: '30rem', margin: '0 auto', padding: '4rem 1.2rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · Sign in
      </div>
      <h1 style={{ fontSize: '1.6rem', margin: '0.3rem 0 0.8rem' }}>Fundamentals of Construction</h1>
      <p style={{ color: 'var(--muted)', margin: '0 0 1.1rem' }}>
        The course modules are open to a list of addresses. Sign in with the Google account whose
        address is on that list; if you are not sure it is, sign in anyway and the next page will
        tell you which address was refused.
      </p>

      <div
        className="card"
        style={{ margin: '0 0 1.2rem', background: 'transparent', fontSize: '0.85rem', color: 'var(--muted)' }}
      >
        <div className="label" style={{ marginBottom: '0.35rem' }}>Terms</div>
        <p style={{ margin: 0 }}>
          The material in these modules — the worked problems, the equipment figures, the simulation
          results and the tutor&apos;s replies — is provided <strong>as is</strong>, for teaching. It
          comes with no warranty of any kind, express or implied, and the author accepts no liability
          for any loss or damage arising from its use. It is not engineering advice and must not be
          relied on for the design, costing or execution of real work. By signing in you accept this.
        </p>
      </div>

      {error ? (
        <p className="card" style={{ borderColor: 'var(--caution)', color: 'var(--caution)', fontSize: '0.9rem', marginBottom: '0.9rem' }}>
          {error}
        </p>
      ) : null}

      <div className="card">
        <GoogleSignInButton next={target} />
      </div>

      <p style={{ color: 'var(--muted)', fontSize: '0.88rem', marginTop: '1.4rem' }}>
        The <Link href="/">construction simulator</Link> is open to everyone and needs no account.
      </p>
    </main>
  );
}
