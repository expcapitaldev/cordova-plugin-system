#import <Cordova/CDVPlugin.h>

@interface SystemPlugin : CDVPlugin
- (void)setTextZoom:(CDVInvokedUrlCommand*)command;
- (void)getAvailableMailClients:(CDVInvokedUrlCommand*)command;
- (void)openEmailApp:(CDVInvokedUrlCommand*)command;
@end
