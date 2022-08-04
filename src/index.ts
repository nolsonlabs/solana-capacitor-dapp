import { registerPlugin } from '@capacitor/core';

import type { SolanaWalletAdaptorPlugin } from './definitions';

const SolanaWalletAdaptor = registerPlugin<SolanaWalletAdaptorPlugin>(
  'SolanaWalletAdaptor',
  {
    web: () => import('./web').then(m => new m.SolanaWalletAdaptorWeb()),
  },
);

export * from './definitions';
export { SolanaWalletAdaptor };
