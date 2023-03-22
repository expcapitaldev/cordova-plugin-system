#import <Cordova/CDVPlugin.h>
#include "CDVReachabilityManager.h"

@interface SystemPlugin : CDVPlugin
@property(strong) NSString* networkInfoCallbackId;
@property (strong, nonatomic) ReachabilityManager *reachabilityManager;
- (void)setTextZoom:(CDVInvokedUrlCommand*)command;
- (void)getAvailableMailClients:(CDVInvokedUrlCommand*)command;
- (void)openEmailApp:(CDVInvokedUrlCommand*)command;
- (void)startNetworkInfoNotifier:(CDVInvokedUrlCommand*)command;
- (void)stopNetworkInfoNotifier:(CDVInvokedUrlCommand*)command;
@end
