# @nolson/solana-wallet-adaptor-capacitor

Capacitor implementation of Solana mobile wallet adaptor

## Install

```bash
npm install @nolson/solana-wallet-adaptor-capacitor
npx cap sync
```

## API

<docgen-index>

* [`listAvailableWallets()`](#listavailablewallets)
* [`isPackageInstalled(...)`](#ispackageinstalled)
* [`connectToWallet(...)`](#connecttowallet)
* [`checkIsWalletEndpointAvailable()`](#checkiswalletendpointavailable)
* [`getCapabilities()`](#getcapabilities)
* [`installedApps()`](#installedapps)
* [`authorize(...)`](#authorize)
* [`reauthorize(...)`](#reauthorize)
* [`deauthorize(...)`](#deauthorize)
* [`signTransactions(...)`](#signtransactions)
* [`authorizeAndSignTransactions(...)`](#authorizeandsigntransactions)
* [`signMessages(...)`](#signmessages)
* [`signAndSendTransactions(...)`](#signandsendtransactions)
* [`requestAirdrop(...)`](#requestairdrop)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

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


### getCapabilities()

```typescript
getCapabilities() => Promise<{ capabilitiesRequested: boolean; }>
```

**Returns:** <code>Promise&lt;{ capabilitiesRequested: boolean; }&gt;</code>

--------------------


### installedApps()

```typescript
installedApps() => Promise<{ installed: string[]; }>
```

**Returns:** <code>Promise&lt;{ installed: string[]; }&gt;</code>

--------------------


### authorize(...)

```typescript
authorize(wallet: string) => Promise<{ authorized: boolean; authToken: string; publicKey: string; connection: any; }>
```

| Param        | Type                |
| ------------ | ------------------- |
| **`wallet`** | <code>string</code> |

**Returns:** <code>Promise&lt;{ authorized: boolean; authToken: string; publicKey: string; connection: any; }&gt;</code>

--------------------


### reauthorize(...)

```typescript
reauthorize(options: { authToken: string; }) => Promise<{ reauthorized: boolean; authToken: string; publicKey: string; }>
```

| Param         | Type                                |
| ------------- | ----------------------------------- |
| **`options`** | <code>{ authToken: string; }</code> |

**Returns:** <code>Promise&lt;{ reauthorized: boolean; authToken: string; publicKey: string; }&gt;</code>

--------------------


### deauthorize(...)

```typescript
deauthorize(options: { authToken: string; connection: any; }) => Promise<{ deauthorized: boolean; }>
```

| Param         | Type                                                 |
| ------------- | ---------------------------------------------------- |
| **`options`** | <code>{ authToken: string; connection: any; }</code> |

**Returns:** <code>Promise&lt;{ deauthorized: boolean; }&gt;</code>

--------------------


### signTransactions(...)

```typescript
signTransactions(options: { count: number; authToken: string; publicKey: string; }) => Promise<{ success: boolean; }>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code>{ count: number; authToken: string; publicKey: string; }</code> |

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------


### authorizeAndSignTransactions(...)

```typescript
authorizeAndSignTransactions(options: { count: number; }) => Promise<{ success: boolean; authToken: string; publicKey: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ count: number; }</code> |

**Returns:** <code>Promise&lt;{ success: boolean; authToken: string; publicKey: string; }&gt;</code>

--------------------


### signMessages(...)

```typescript
signMessages(options: { count: number; authToken: string; }) => Promise<{ success: boolean; }>
```

| Param         | Type                                               |
| ------------- | -------------------------------------------------- |
| **`options`** | <code>{ count: number; authToken: string; }</code> |

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------


### signAndSendTransactions(...)

```typescript
signAndSendTransactions(options: { count: number; authToken: string; }) => Promise<{ success: boolean; }>
```

| Param         | Type                                               |
| ------------- | -------------------------------------------------- |
| **`options`** | <code>{ count: number; authToken: string; }</code> |

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------


### requestAirdrop(...)

```typescript
requestAirdrop(options: { authToken: string; }) => Promise<{ success: boolean; }>
```

| Param         | Type                                |
| ------------- | ----------------------------------- |
| **`options`** | <code>{ authToken: string; }</code> |

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------

</docgen-api>
