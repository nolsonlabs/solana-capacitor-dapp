export interface SolanaWalletAdaptorPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  listAvailableWallets(): Promise<{ wallets: string[] }>;
  isPackageInstalled(options: { value: string }): Promise<{ installed: boolean }>;
  connectToWallet(walletName: string): Promise<{ connected: boolean }>;
  checkIsWalletEndpointAvailable(): Promise<{endpointAvailable: boolean}>;
}

