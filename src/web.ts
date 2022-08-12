import { WebPlugin } from '@capacitor/core';
// import { WalletAdapter, WalletReadyState } from '@solana/wallet-adapter-base';
import { SolanaMobileWalletAdapter,
        createDefaultAddressSelector,
        createDefaultAuthorizationResultCache,
      } from '@solana-mobile/wallet-adapter-mobile';
import {
    PhantomWalletAdapter,
    SlopeWalletAdapter,
    SolflareWalletAdapter,
} from '@solana/wallet-adapter-wallets';
// import { clusterApiUrl, Cluster } from '@solana/web3.js';

import type { SolanaWalletAdaptorPlugin } from './definitions';

export class SolanaWalletAdaptorWeb
  extends WebPlugin
  implements SolanaWalletAdaptorPlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async listAvailableWallets(): Promise<{ wallets: string[] }> {
    const phantom = new PhantomWalletAdapter().readyState;
    console.log('Phantom object');
    console.log(phantom);
    const solflare = new SolflareWalletAdapter().readyState;
    console.log('Solflare object');
    console.log(solflare);
    const slope = new SlopeWalletAdapter().readyState;
    const wallets: string[] = [phantom, solflare, slope];
    return {wallets};
  }

  async connectToWallet(walletName: string): Promise<{ connected: boolean }> {
    if (walletName === 'phantom') {
      // connect to phantom
      const app = new SolanaMobileWalletAdapter({
        addressSelector: createDefaultAddressSelector(),
        appIdentity: {
            name: 'Phantom test',
            uri: 'app.phantom',
            //uri: 'https://phantom.app/ul/v1/connect',
            // icon: './favicon.png',
       },
        authorizationResultCache: createDefaultAuthorizationResultCache(),
        cluster: 'devnet'
    });

     await app.connect();

    console.log(app);
      const phantom = new PhantomWalletAdapter();
      // await phantom.connect();

      console.log(phantom.publicKey);
      console.log(phantom);
    }
    return {connected: true};
  }

  async disconnectWallet(walletName: string): Promise<string> {
    // Disconnect wallet
    return walletName;
  }

  async isPackageInstalled(options: { value: string }): Promise<{ installed: boolean }> {
    console.log(options);
    return { installed: true };
  }

  async checkIsWalletEndpointAvailable(): Promise<{endpointAvailable: boolean}> {
    return { endpointAvailable: false};
  }
}
