import Foundation

@objc public class SolanaWalletAdaptor: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
