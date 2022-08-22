import type { Wallet } from './wallet.interface';

export interface WalletInfo {
    installed: Wallet[],
    deeplink: Wallet[],
    notInstalled: Wallet[]
}