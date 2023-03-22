#include <ifaddrs.h>
#include <arpa/inet.h>
#import <SystemConfiguration/SystemConfiguration.h>
#import <CPReachability/Reachability.h>
// SWIFT_MODULE_NAME
#import "StationTradingModule-Swift.h"



@interface ProxyInfo : NSObject
@property(strong) NSString* _Nonnull type;
@property(strong) NSString* _Nullable host;
@property(strong) NSString* _Nullable port;
@property(strong) NSString* _Nullable urlKey;
@end

@interface NetworkInfo : NSObject
@property(strong) NSArray<ProxyInfo *>* _Nullable proxy;
@property(strong) NSArray<NetworkAddress*>* _Nullable addresses;
@property(strong) NSString* _Nullable transport;
@end

typedef void (^SPNetworkInfoBlock) (NetworkInfo * _Nonnull info);

@interface ReachabilityManager : NSObject
@property BOOL reachabilityListenerStarted;
@property(strong) CPReachability* _Nullable reach;
@property(strong, nonatomic) NSArray<ProxyInfo*>* _Nullable prevProxiesInfo;
@property(strong, nonatomic) NSArray<NetworkAddress*>* _Nullable prevNetworkAddress;
@property(copy) NSString* _Nullable prevTransport;
@property (copy, nonatomic) SPNetworkInfoBlock _Nullable networkInfoBlock;
@property(strong) NSURL* _Nullable proxyUrl;
- (void)start:(NSURL*_Nonnull)url :(SPNetworkInfoBlock _Nonnull)completionHandler;
- (void)stop;
@end


