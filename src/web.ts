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
// import { Connection } from '@solana/web3.js';
// import { clusterApiUrl, Cluster } from '@solana/web3.js';

import type { SolanaWalletAdaptorPlugin } from './definitions';

export class SolanaWalletAdaptorWeb
  extends WebPlugin
  implements SolanaWalletAdaptorPlugin
{

  async listAvailableWallets(): Promise<{ wallets: string[] }> {
    const phantom = new PhantomWalletAdapter().readyState;
    const solflare = new SolflareWalletAdapter().readyState;
    const slope = new SlopeWalletAdapter().readyState;
    const wallets: string[] = [phantom, solflare, slope];
    return {wallets};
  }

  
  async installedApps(): Promise<{ installed: string[]}> {
    const phantom = new PhantomWalletAdapter().readyState;
    const solflare = new SolflareWalletAdapter().readyState;
    const slope = new SlopeWalletAdapter().readyState;
    const outputArr = [];
    if (phantom == 'Installed') {
      outputArr.push('app.phantom');
    }
    if (solflare == 'Installed') {
      outputArr.push('com.solflare.mobile')
    }
    if (slope == 'Installed') {
      outputArr.push('com.y8.slope')
    }


    return { installed: outputArr }
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


    }
    return {connected: true};
  }

  async disconnectWallet(walletName: string): Promise<string> {
    // Disconnect wallet
    return walletName;
  }

  async isPackageInstalled(): Promise<{ installed: boolean }> {
    return { installed: true };
  }

  async checkIsWalletEndpointAvailable(): Promise<{endpointAvailable: boolean}> {
    return { endpointAvailable: false};
  }

  async getCapabilities(): Promise<{ capabilitiesRequested: boolean}> {
    return { capabilitiesRequested: true }
  }
  async authorize(wallet: string): Promise<{    
    authorized: boolean,
    authToken: string,
    publicKey: any,
    connection: any
    }> {
      if (wallet === 'Phantom') {
        const phantom = new PhantomWalletAdapter();
        await phantom.connect()
        phantom.publicKey
        return { authorized: true, authToken: '', publicKey: phantom.publicKey, connection: phantom};
      }

    return { authorized: true, authToken: '', publicKey: '', connection: null};
  }

  async reauthorize(): Promise<{     
    reauthorized: boolean,
    authToken: string,
    publicKey: any}> {
    return { reauthorized: true, authToken: '', publicKey: '' }
  }

  async deauthorize(options: {authToken: string, connection: PhantomWalletAdapter}): Promise<{ 
    deauthorized: boolean
   }> {
     console.log('Connection');
     options.connection.disconnect();    
     return { deauthorized: true };

   }





  async signTransactions() : Promise<{success: boolean}> {
    return {success: true};
  }

  async authorizeAndSignTransactions() : Promise<{success: boolean, authToken: string, publicKey: string}> {
    return {success: true, authToken: '', publicKey: ''}
  }

  async signMessages() : Promise<{success: boolean}> {
     return {success: true}
   }

   async signAndSendTransactions() : Promise<{success: boolean}> {
    return {success: true}
  }

  async requestAirdrop() : Promise<{success: boolean}> {
    return {success: true}
  }
}
