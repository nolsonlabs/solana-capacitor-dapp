import { WebPlugin } from '@capacitor/core';
import { Device } from '@capacitor/device';
import type { PhantomWalletAdapter} from '@solana/wallet-adapter-wallets';

import type { SolanaWalletAdaptorPlugin } from './definitions';
import { WalletDescriptions } from './wallet.descriptions';
import type { WalletInfo } from './walletinfo.interface';

export class SolanaWalletAdaptorWeb
  extends WebPlugin
  implements SolanaWalletAdaptorPlugin
{
  
  async getWalletAndEnvironmentInfo(): Promise<{ dAppPlatform: string, dAppOs: string, walletInfo: WalletInfo[]}> {
    const walletInfo: any[] = [];
    const walletDescriptions = (new WalletDescriptions).wallets;
    Object.keys(walletDescriptions).forEach((walletKey: string) => {
      const wallet = walletDescriptions[walletKey];
      const instance = wallet.walletAdapter;
      let installed = false;
      if (instance.readyState == 'Installed') {
        installed = true;
      }
      const walletInfoObject = {
        walletName: wallet.walletName,
        walletInstalled: installed,
        walletHasDeepLinkCapability: wallet.walletHasDeeplinkCapability,
        walletIcon: wallet.walletIcon
      }

      walletInfo.push(walletInfoObject);

    });

    const deviceInfo = await Device.getInfo();
    return { 
      dAppPlatform: deviceInfo.platform,
      dAppOs: deviceInfo.operatingSystem,
      walletInfo: walletInfo
    }
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
      const walletDescriptions = (new WalletDescriptions).wallets;
      const adapter = walletDescriptions[wallet.toLowerCase()].walletAdapter;

      try {
        await adapter.connect();
        return { authorized: true, authToken: '', publicKey: adapter.publicKey, connection: adapter};
      } catch (error) {
        return { authorized: false, authToken: '', publicKey: '', connection: null};
        }
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
