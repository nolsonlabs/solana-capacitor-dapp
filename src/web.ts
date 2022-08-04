import { WebPlugin } from '@capacitor/core';

import type { SolanaWalletAdaptorPlugin } from './definitions';

export class SolanaWalletAdaptorWeb
  extends WebPlugin
  implements SolanaWalletAdaptorPlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
