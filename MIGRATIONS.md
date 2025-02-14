Troubleshooting Migrations
==========

Migration to Version 1.13
---------------------------------
Update usages of `z.cash.ecc.android.sdk.model.LightWalletEndpoint` to `co.electriccoin.lightwallet.client.model.LightWalletEndpoint`.

SDK clients should avoid using generated GRPC objects, as these are an internal implementation detail and are in process of being removed from the public API.  Any clients using GRPC objects will find these have been repackaged from `cash.z.wallet.sdk.rpc` to `cash.z.wallet.sdk.internal.rpc` to signal they are not a public API.

Migration to Version 1.12
---------------------------------
`TransactionOverview`, `Transaction.Sent`, and `Transaction.Received` have been updated to reflect that `minedHeight` is nullable.

Migration to Version 1.11
---------------------------------
The way the SDK is initialized has changed.  The `Initializer` object has been removed and `Synchronizer.new` now takes a longer parameter list which includes the parameters previously passed to `Initializer`.

SDK initialization also now requires access to the seed bytes at two times: 1. during new wallet creation and 2. during upgrade of an existing wallet to SDK 1.10 due to internal data migrations.  To handle case #2, client should wrap `Synchronizer.new()` with a try-catch for `InitializerException.SeedRequired`.  Clients can pass `null` to try to initialize the SDK without the seed, then try again if the exception is thrown to indicate the seed is needed.  This pattern future-proofs initialization, as the seed may be required by future SDK updates.

`Synchronizer.stop()` has been removed.  `Synchronizer.new()` now returns an instance that implements the `Closeable` interface.  This effectively means that calls to `stop()` are replaced with `close()`.  This change also enables greater safety within client applications, as the Closeable interface can be hidden from global synchronizer instances.  For exmaple:
```
val synchronizerFlow: Flow<Synchronizer> = callbackFlow<Synchronizer> {
   val closeableSynchronizer: CloseableSynchronizer = Synchronizer.new(...)

    send(closeableSynchronizer)
    awaitClose {
        closeableSynchronizer.close()
    }
}
```

To improve type safety of the public API, Zcash account indexes are now represented by an `Account` object.  The SDK currently only supports the default account, `Account.DEFAULT`.  Migration will effectively require replacing APIs with an account `0` with `Account.DEFAULT`.

To support Network Upgrade 5, the way keys are generated has changed.

For SDK clients that regenerate the keys from a mnemonic, migration might look like:
 * Replace usage of `UnifiedViewingKey` with `UnifiedFullViewingKey`
 * Replace `DerivationTool.deriveUnifiedViewingKeys` with `DerivationTool.deriveUnifiedFullViewingKeys`

For SDK clients that store the key separately from the mnemonic, the migration might look like:
 * Replace usage of `UnifiedViewingKey(extfvk: String, extpub: String)` with `UnifiedFullViewingKey(encoding: String)`
 * Replace `DerivationTool.deriveUnifiedViewingKeys` with `DerivationTool.deriveUnifiedFullViewingKeys`
 * Delete any previously persisted values for `UnifiedViewingKey(extfvk: String, extpub: String)`,
   provided that they can be rederived from the mnemonic.
 * Re-generate the key from the mnemonic using `DerivationTool.deriveUnifiedFullViewingKeys`

To support Unified Addresses (UAs), some APIs have been modified.  In particular, `Synchronizer.getUnifiedAddress()` returns the unified address while `Synchronizer.getSaplingAddress()` and `Synchronizer.getTransparentAddress()` return the sapling or transparent components of the unified address.  Due to this change and the derivation of different addresses from UAs, clients may notice that the transparent address returned by this API is different from the transparent address returned by older versions of the SDK.  Note that UA support does not yet encompass orchard addresses.

Due to internal changes in the SDK, the way transactions are queried and represented works differently.  The previous ConfirmedTransaction object has been replaced with `TransactionOverview` which contains less information.  Missing fields, such as memos and recipients, can be queried with `Synchronizer.getMemos(TransactionOverview)` and `Synchronizer.getReceipients(TransactionOverview)`.

Migration to Version 1.9
--------------------------------------
`ZcashNetwork` is no longer an enum. The prior enum values are now declared as object properties `ZcashNetwork.Mainnet` and `ZcashNetwork.Testnet`.  For the most part, this change should have minimal impact.  ZcashNetwork was also moved from the package `cash.z.ecc.android.sdk.type` to `cash.z.ecc.android.sdk.model`, which will require a change to your import statements.  The server fields have been removed from `ZcashNetwork`, allowing server and network configuration to be done independently.

`LightWalletEndpoint` is a new object to represent server information.  Default values can be obtained from `LightWalletEndpoint.defaultForNetwork(ZcashNetwork)`

`Synchronizer` no longer allows changing the endpoint after construction.  Instead, construct a new `Synchronizer` with the desired endpoint.

Migration to Version 1.8 from 1.7
--------------------------------------
Various APIs used `Int` to represent network block heights.  Those APIs now use a typesafe `BlockHeight` type.  BlockHeight is constructed with a factory method `BlockHeight.new(ZcashNetwork, Long)` which uses the network to validate the height is above the network's sapling activation height.

`WalletBirthday` has been renamed to `Checkpoint` and removed from the public API.  Where clients previously passed in a `WalletBirthday` object, now a `BlockHeight` can be passed in instead.

Migration to Version 1.7 from 1.6
--------------------------------------
Various APIs used `Long` value to represent Zatoshi currency amounts.  Those APIs now use a typesafe `Zatoshi` class.  When passing amounts, simply wrap Long values with the Zatoshi constructor `Zatoshi(Long)`.  When receiving values, simply unwrap Long values with `Zatoshi.value`.

`WalletBalance` no longer has uninitialized default values.  This means that `Synchronizer` fields that expose a WalletBalance now use `null` to signal an uninitialized value.  Specifically this means `Synchronizer.orchardBalances`, `Synchronzier.saplingBalances`, and `Synchronizer.transparentBalances` have nullable values now.

`WalletBalance` has been moved from the package `cash.z.ecc.android.sdk.type` to `cash.z.ecc.android.sdk.model` 

`ZcashSdk.ZATOSHI_PER_ZEC` has been moved to `Zatoshi.ZATOSHI_PER_ZEC`.

`ZcashSdk.MINERS_FEE_ZATOSHI` has been renamed to `ZcashSdk.MINERS_FEE` and the type has changed from `Long` to `Zatoshi`.

Migrating to Version 1.4.* from 1.3.*
--------------------------------------
The main entrypoint to the SDK has changed.

Previously, a Synchronizer was initialized with `Synchronizer(initializer)` and now it is initialized with `Synchronizer.new(initializer)` which is also now a suspending function.  Helper methods `Synchronizer.newBlocking()` and `Initializer.newBlocking()` can be used to ease the transition.

For clients needing more complex initialization, the previous default method arguments for `Synchronizer()` were moved to `DefaultSynchronizerFactory`.

The minimum Android version supported is now API 19.

Migrating to Version 1.3.0-beta18 from 1.3.0-beta19
--------------------------------------
Various APIs that have always been considered private have been moved into a new package called `internal`.  While this should not be a breaking change, clients that might have relied on these internal classes should stop doing so.  If necessary, these calls can be migrated by changing the import to the new `internal` package name.

A number of methods have been converted to suspending functions, because they were performing slow or blocking calls (e.g. disk IO) internally.  This is a breaking change.

Migrating to Version 1.3.* from 1.2.*
--------------------------------------
The biggest breaking changes in 1.3 that inspired incrementing the minor version number was simplifying down to one "network aware" library rather than two separate libraries, each dedicated to either testnet or mainnet. This greatly simplifies the gradle configuration and has lots of other benefits. Wallets can now set a network with code similar to the following:

```kotlin
// Simple example
val network: ZcashNetwork = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet

// Dependency Injection example
@Provides @Singleton fun provideNetwork(): ZcashNetwork = ZcashNetwork.Mainnet
```
1.3 also adds a runtime check for wallets that are accessing properties before the synchronizer has started. By introducing a `prepare` step, we are now able to catch these errors proactively rather than allowing them to turn into subtle bugs that only surface later. We found this when code was accessing properties before database migrations completed, causing undefined results. Developers do not need to make any changes to enable these checks, they happen automatically and result in detailed error messages.

| Error                           | Issue                               | Fix                      |
| ------------------------------- | ----------------------------------- | ------------------------ |
| No value passed for parameter 'network' | Many functions are now network-aware | pass an instance of ZcashNetwork, which is typically set during initialization |
| Unresolved reference: validate  | The `validate` package was removed  | instead of `cash.z.ecc.android.sdk.validate.AddressType`<br/>import `cash.z.ecc.android.sdk.type.AddressType`  |
| Unresolved reference: WalletBalance | WalletBalance was moved out of `CompactBlockProcessor` and up to the `type` package  | instead of `cash.z.ecc.android.sdk.CompactBlockProcessor.WalletBalance`<br/>import `cash.z.ecc.android.sdk.type.WalletBalance`  |
| Unresolved reference: server  | This was replaced by `setNetwork` | instead of `config.server(host, port)`<br/>use `config.setNetwork(network, host, port)` |
| Unresolved reference: balances  | 3 types of balances are now exposed | change `balances` to `saplingBalances` |
| Unresolved reference: latestBalance  | There are now multiple balance types so this convenience function was removed in favor of forcing wallets to think about which balances they want to show.  | In most cases, just use `synchronizer.saplingBalances.value` directly, instead |
| Type mismatch: inferred type is String but ZcashNetwork was expected  | This function is now network aware | use `Initializer.erase(context, network, alias)` |
| Type mismatch: inferred type is Int? but ZcashNetwork was expected | This function is now network aware | use `WalletBirthdayTool.loadNearest(context, network, height)` instead |
| None of the following functions can be called with the arguments supplied: <br/>public open fun deriveShieldedAddress(seed: ByteArray, network: ZcashNetwork, accountIndex: Int = ...): String defined in cash.z.ecc.android.sdk.tool.DerivationTool.Companion<br/>public open fun deriveShieldedAddress(viewingKey: String, network: ZcashNetwork): String defined in cash.z.ecc.android.sdk.tool.DerivationTool.Companion | This function is now network aware | use `deriveShieldedAddress(seed, network)`|
