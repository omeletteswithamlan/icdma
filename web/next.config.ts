import type { NextConfig } from 'next';

// The /learn syllabus and Module 1 went public on 2026-09-05 (Amlan's call).
// Set HIDE_LEARN=1 on a deployment to take the whole section private again;
// unfinished modules are marked 'soon' in lib/takeaways.ts and are not linked.
const showLearn = process.env.NODE_ENV === 'development' || process.env.HIDE_LEARN !== '1';

const nextConfig: NextConfig = {
  transpilePackages: ['icdma-engine'],
  env: {
    NEXT_PUBLIC_SHOW_LEARN: showLearn ? '1' : '0',
  },
  async redirects() {
    if (showLearn) return [];
    return [
      { source: '/learn', destination: '/', permanent: false },
      { source: '/learn/:path*', destination: '/', permanent: false },
    ];
  },
};

export default nextConfig;
