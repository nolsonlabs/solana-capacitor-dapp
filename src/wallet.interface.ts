import type { SolanaMobileWalletAdapter } from '@solana-mobile/wallet-adapter-mobile';
import type { PhantomWalletAdapter,
         SolflareWalletAdapter,
         SlopeWalletAdapter
        } from '@solana/wallet-adapter-wallets';

export interface Wallet {
    walletName: string,
    walletAndroidPackageId: string,
    walletAdapter: SolanaMobileWalletAdapter | PhantomWalletAdapter | SolflareWalletAdapter | SlopeWalletAdapter,
    walletIsInstalled: boolean,
    walletHasDeeplinkCapability: boolean,
    walletIcon: string;
}