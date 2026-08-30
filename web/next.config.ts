import type { NextConfig } from 'next';

// The /learn modules are under construction: visible in local dev, hidden in
// production until SHOW_LEARN=1 is set on the deployment.
const showLearn = process.env.NODE_ENV === 'development' || process.env.SHOW_LEARN === '1';

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
