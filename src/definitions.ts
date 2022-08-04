export interface SolanaWalletAdaptorPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
