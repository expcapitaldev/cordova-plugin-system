fileprivate extension String {
    func isContainsPrefix(in array: [String]) -> Bool {
        array.contains(where: {self.hasPrefix($0)})
    }
}

@objc(ReachabilityIPHelper)
class ReachabilityIPHelper : NSObject {

    @objc
    class NetworkAddress: NSObject {
        @objc var interface: String
        @objc var ip: String
        // var netmask: String
        init(interface: String, ip: String) {
            self.interface = interface
            self.ip = ip
        }
        override func isEqual(_ object: Any?) -> Bool {
            guard let toCompare = object as? NetworkAddress else {
                return false
            }
            return interface == toCompare.interface && ip == toCompare.ip
        }
    }

    private struct InterfaceNames {
//        static let wifi = ["en0"]
//        static let wired = ["en2", "en3", "en4"]
//        static let cellular = ["pdp_ip0","pdp_ip1","pdp_ip2","pdp_ip3"]
//        static let vpn = ["tap","tun","ipsec","ppp", "utun"]
//        static let supported = wifi + wired + vpn + cellular
        static let supported = ["en", "pdp_ip", "tap", "tun", "ipsec", "ppp", "utun"]
    }

    @objc
    static func getNetworkAddress() -> [NetworkAddress] {
        var addresses = [NetworkAddress]()
        var addrList : UnsafeMutablePointer<ifaddrs>?
        guard
            getifaddrs(&addrList) == 0,
            let firstAddr = addrList
        else {
            addresses.append(NetworkAddress(interface: "", ip: ""))
            return addresses
        }
        defer { freeifaddrs(addrList) }
        for cursor in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            let interfaceName = String(cString: cursor.pointee.ifa_name)
            print(interfaceName)
            var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            if
                interfaceName.isContainsPrefix(in: InterfaceNames.supported),
                let addr = cursor.pointee.ifa_addr,
                getnameinfo(addr, socklen_t(addr.pointee.sa_len), &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST) == 0
            {
                let addrStr = String(cString: hostname)
                if
                    // Note IPv6 networks adds the zone interface name ("%en0") to link-local IP addresses https://developer.apple.com/forums/thread/710449
                    addrStr != "02:00:00:00:00:00",
                    !addrStr.isEmpty
                {
                    // var netmaskName = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    // if
                        // let subnet = cursor.pointee.ifa_netmask,
                        // getnameinfo(subnet, socklen_t(subnet.pointee.sa_len), &netmaskName, socklen_t(netmaskName.count), nil, socklen_t(0), NI_NUMERICHOST) == 0
                    // {
                        // let netmask = String(cString: netmaskName)
//                         addresses.append(NetInfo(interface: interfaceName, ip: addrStr, netmask: netmask ))
                        addresses.append(NetworkAddress(interface: interfaceName, ip: addrStr))
                        print(interfaceName, addrStr)

                   // }

                }
            }

        }

        return addresses
    }
}
