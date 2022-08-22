import type { WalletInfo } from './walletinfo.interface';

export interface SolanaWalletAdaptorPlugin {

  // Fakewallet specific
  checkIsWalletEndpointAvailable(): Promise<{endpointAvailable: boolean}>;
  getCapabilities(): Promise<{ capabilitiesRequested: boolean}>;

  // Cross platform
  getWalletAndEnvironmentInfo(): Promise<{
    dAppPlatform: string,
    dAppOs: string,
    walletInfo: WalletInfo[]
  }>;
  authorize(wallet: string): Promise<{ 
    authorized: boolean,
    authToken: string,
    publicKey: string,
    connection: any;
   }>;
  reauthorize(options: {authToken: string}): Promise<{ 
    reauthorized: boolean,
    authToken: string,
    publicKey: string
   }>;
  deauthorize(options: {authToken: string, connection: any}): Promise<{ 
    deauthorized: boolean
   }>;
  signTransactions(options: {count: number, authToken: string, publicKey: string}) : Promise<{
     success: boolean
    }>;
  authorizeAndSignTransactions(options: {count: number}) : Promise<{
      success: boolean, authToken: string, publicKey: string
     }>;
  signMessages(options: {count: number, authToken: string}) : Promise<{
      success: boolean
     }>;
  signAndSendTransactions(options: {count: number, authToken: string}) : Promise<{
      success: boolean
     }>;
  requestAirdrop(options: {authToken: string}) : Promise<{
      success: boolean
     }>;

}

