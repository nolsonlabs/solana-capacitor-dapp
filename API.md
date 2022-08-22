<docgen-index>

* [`checkIsWalletEndpointAvailable()`](#checkiswalletendpointavailable)
* [`getCapabilities()`](#getcapabilities)
* [`getWalletAndEnvironmentInfo()`](#getwalletandenvironmentinfo)
* [`authorize(...)`](#authorize)
* [`reauthorize(...)`](#reauthorize)
* [`deauthorize(...)`](#deauthorize)
* [`signTransactions(...)`](#signtransactions)
* [`authorizeAndSignTransactions(...)`](#authorizeandsigntransactions)
* [`signMessages(...)`](#signmessages)
* [`signAndSendTransactions(...)`](#signandsendtransactions)
* [`requestAirdrop(...)`](#requestairdrop)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

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


### getWalletAndEnvironmentInfo()

```typescript
getWalletAndEnvironmentInfo() => Promise<{ dAppPlatform: string; dAppOs: string; walletInfo: WalletInfo[]; }>
```

**Returns:** <code>Promise&lt;{ dAppPlatform: string; dAppOs: string; walletInfo: WalletInfo[]; }&gt;</code>

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


### Interfaces


#### WalletInfo

| Prop               | Type                  |
| ------------------ | --------------------- |
| **`installed`**    | <code>Wallet[]</code> |
| **`deeplink`**     | <code>Wallet[]</code> |
| **`notInstalled`** | <code>Wallet[]</code> |


#### Wallet

| Prop                 | Type                |
| -------------------- | ------------------- |
| **`uuid`**           | <code>string</code> |
| **`chain_name`**     | <code>string</code> |
| **`public_address`** | <code>string</code> |

</docgen-api>