#import "CDVReachabilityManager.h"

@implementation ProxyInfo
- (BOOL)isEqual:(id)object {
    ProxyInfo *toCompare = (ProxyInfo *)object;
    BOOL isEqual = NO;
    if (toCompare) {
        isEqual = [toCompare.type isEqualToString:self.type] && (toCompare.host == self.host || [toCompare.host isEqualToString:self.host]) && (toCompare.port == self.port || [toCompare.port isEqualToString:self.port]) && (toCompare.urlKey == self.urlKey || [toCompare.urlKey isEqualToString:self.urlKey]);
    }
    return isEqual;
}
@end

@implementation NetworkInfo
@end

@implementation ReachabilityManager


- (void)start:(NSURL*)url :(SPNetworkInfoBlock)completionHandler {
    self.proxyUrl = url;
    self.networkInfoBlock = completionHandler;
    [self startReachabilityListener];
}

- (void)stop {
    [self stopReachabilityListener];
}

- (void)startReachabilityListener {
    if (self.reachabilityListenerStarted == YES) {
        return;
    }
    self.reachabilityListenerStarted = YES;

    self.reach = [CPReachability reachabilityForInternetConnection];

    __weak ReachabilityManager* weakSelf = self;
    self.reach.reachableBlock = ^(CPReachability*reach)
    {
        // NSLog(@"===qqq REACHABLE!");
        [weakSelf checkNetworkData];
    };

//    self.reach.unreachableBlock = ^(CPReachability*reach)
//    {
//        NSLog(@"===qqq UNREACHABLE!");
//   };
    [self.reach startNotifier];
    [self checkNetworkData];

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause) name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume) name:UIApplicationWillEnterForegroundNotification object:nil];
}

- (void)stopReachabilityListener {
    if (!self.reachabilityListenerStarted) {
        return;
    }
    self.reachabilityListenerStarted = NO;
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [self.reach stopNotifier];
    self.reach = NULL;
    self.networkInfoBlock = NULL;
    self.proxyUrl = NULL;
    self.prevNetworkAddress = NULL;
    self.prevTransport = NULL;
    self.prevProxiesInfo = NULL;
}

- (void)onPause
{
    if (!self.reachabilityListenerStarted) {
        return;
    }
    [self.reach stopNotifier];
}

- (void)onResume
{
    if (!self.reachabilityListenerStarted) {
        return;
    }
    [self.reach startNotifier];
    [self checkNetworkData];
}

- (void) checkNetworkData {

    if (!self.reachabilityListenerStarted) {
        return;
    }

    if (!self.reach || !self.reach.isReachable) {
        return;
    }

    NSArray<NetworkAddress *> * currentNetworkAddresses = [ReachabilityIPHelper getNetworkAddress];
    NSArray<ProxyInfo *> * currentProxiesInfo = [self getProxySettings];
    NSString* currentTransport = [self getCurrentNetworkStatus];

	BOOL isNetworkAddressChanged = ![self.prevNetworkAddress isEqualToArray:currentNetworkAddresses];
	BOOL isProxyInfoChanged = ![self.prevProxiesInfo isEqualToArray:currentProxiesInfo];
	BOOL isTransportChanged = ![self.prevTransport isEqualToString:currentTransport];

    NetworkInfo * networkInfo = [[NetworkInfo alloc] init];
    if (isNetworkAddressChanged) {
        networkInfo.addresses = currentNetworkAddresses;
        self.prevNetworkAddress = currentNetworkAddresses;
    }
    if (isProxyInfoChanged) {
        networkInfo.proxy = currentProxiesInfo;
        self.prevProxiesInfo = currentProxiesInfo;
    }
    if (isTransportChanged) {
        networkInfo.transport = currentTransport;
        self.prevTransport = currentTransport;
    }
    if (isNetworkAddressChanged || isProxyInfoChanged || isTransportChanged) {
        self.networkInfoBlock(networkInfo);
    }
}

- (NSArray<ProxyInfo*>*) getProxySettings {

    CFDictionaryRef proxySettingsRef = CFNetworkCopySystemProxySettings();
    CFURLRef urlRef = (__bridge CFURLRef)self.proxyUrl;
    CFArrayRef proxiesRef = CFNetworkCopyProxiesForURL(urlRef, proxySettingsRef);
    NSArray<ProxyInfo*>* proxyInfo = [self createProxiesArray:(__bridge NSArray*)proxiesRef];

    CFRelease(proxySettingsRef);
    CFRelease(proxiesRef);

    return proxyInfo;
}

- (NSArray<ProxyInfo*>*) createProxiesArray:(NSArray*)proxies
{
    NSMutableArray *returnArray = [NSMutableArray<ProxyInfo *> arrayWithCapacity:[proxies count]];
    if([proxies count] == 0)
    {
        ProxyInfo * proxyInfo = [[ProxyInfo alloc] init];
        proxyInfo.type = (__bridge NSString*) kCFProxyTypeNone;
        [returnArray addObject: proxyInfo];
    }
    else
    {

        for (NSDictionary *proxyInfo in proxies) {
            [returnArray addObject: [self createProxyInformation:proxyInfo]];
        }

    }
    return [returnArray copy];
}

- (ProxyInfo*) createProxyInformation:(NSDictionary*)proxy
{
    NSString * type = [proxy objectForKey:(__bridge NSString*) kCFProxyTypeKey];
    NSString * host = [proxy objectForKey:(__bridge NSString*) kCFProxyHostNameKey];
    NSString * port = [proxy objectForKey:(__bridge NSString*) kCFProxyPortNumberKey];
    NSURL * urlKey = [proxy objectForKey:(__bridge NSURL*) kCFProxyAutoConfigurationURLKey];


    ProxyInfo * proxyInfo = [[ProxyInfo alloc] init];

    proxyInfo.type = type != nil ? type : (__bridge NSString*) kCFProxyTypeNone;

    if (host != nil) {
        proxyInfo.host = host;
    }
    if (port != nil) {
        proxyInfo.port =port;
    }
    if (urlKey && urlKey.class == [NSURL class] && urlKey.absoluteString) {
        proxyInfo.urlKey = urlKey.absoluteString;
    }
    return proxyInfo;
}

- (NSString*) getCurrentNetworkStatus
{
    if (!self.reach || !self.reach.isReachable) {
        return @"UNKNOWN";
    }

    NetworkStatus networkStatus = [self.reach currentReachabilityStatus];
    switch (networkStatus) {
        case ReachableViaWiFi:
            return @"WIFI";
        case ReachableViaWWAN:
            return @"CELLULAR";
        default:
            return @"UNKNOWN";

    }
}


@end
