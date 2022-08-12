import nodeResolve from '@rollup/plugin-node-resolve';
import nodePolyfills from 'rollup-plugin-polyfill-node';

export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorSolanaWalletAdaptor',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
      plugins: [
        nodeResolve({exportConditions: ['node']}),
        nodePolyfills({include: ['crypto']})
      ]
    },
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  external: ['@capacitor/core', '@solana/wallet-adapter-wallets'],
};
