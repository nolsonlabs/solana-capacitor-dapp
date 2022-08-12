# @nolson/solana-wallet-adaptor-capacitor

Capacitor implementation of Solana mobile wallet adaptor

## Install

```bash
npm install @nolson/solana-wallet-adaptor-capacitor
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`listAvailableWallets()`](#listavailablewallets)
* [`isPackageInstalled(...)`](#ispackageinstalled)
* [`connectToWallet(...)`](#connecttowallet)
* [`checkIsWalletEndpointAvailable()`](#checkiswalletendpointavailable)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### listAvailableWallets()

```typescript
listAvailableWallets() => Promise<{ wallets: string[]; }>
```

**Returns:** <code>Promise&lt;{ wallets: string[]; }&gt;</code>

--------------------


### isPackageInstalled(...)

```typescript
isPackageInstalled(options: { value: string; }) => Promise<{ installed: boolean; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ installed: boolean; }&gt;</code>

--------------------


### connectToWallet(...)

```typescript
connectToWallet(walletName: string) => Promise<{ connected: boolean; }>
```

| Param            | Type                |
| ---------------- | ------------------- |
| **`walletName`** | <code>string</code> |

**Returns:** <code>Promise&lt;{ connected: boolean; }&gt;</code>

--------------------


### checkIsWalletEndpointAvailable()

```typescript
checkIsWalletEndpointAvailable() => Promise<{ endpointAvailable: boolean; }>
```

**Returns:** <code>Promise&lt;{ endpointAvailable: boolean; }&gt;</code>

--------------------

</docgen-api>
