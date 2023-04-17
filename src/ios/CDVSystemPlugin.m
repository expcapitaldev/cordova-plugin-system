#import "CDVSystemPlugin.h"

static NSString*const LOG_TAG = @"SystemPlugin[native]";

//@interface MailClient : NSObject
//@property(nonatomic, strong) NSString *title;
//@property(nonatomic, strong) NSString *scheme;
//@end
//
//@implementation MailClient
//@end

@implementation SystemPlugin

// @override abstract
- (void)pluginInitialize {
    NSLog(@"Starting SystemPlugin");
}

- (void)onReset {

    [self.commandDelegate runInBackground:^{
        @try {
            self.networkInfoCallbackId = NULL;
            if (self.reachabilityManager != NULL) {
                [self.reachabilityManager stop];
                self.reachabilityManager = NULL;
            }
        }@catch (NSException *exception) {
            [self handlePluginExceptionWithoutContext:exception];
        }
    }];

    dispatch_async(dispatch_get_main_queue(), ^{

        @try {
            if (self.securityView == nil) {
                return;
            }
            UIViewController* rootController = [UIApplication sharedApplication].delegate.window.rootViewController;
            UIView *topView = rootController.view;

            [self.securityView removeFromSuperview];
            self.securityView = nil;
            [topView insertSubview:self.webView atIndex:1];
            [topView setNeedsLayout];
        }@catch (NSException *exception) {
            [self handlePluginExceptionWithoutContext:exception];
        }

    });
}

#pragma mark - plugin API
- (void) setTextZoom:(CDVInvokedUrlCommand*)command {

    [self.commandDelegate runInBackground:^{
        @try {

            double zoom = [[command.arguments objectAtIndex:0] doubleValue];

            NSString *jsString = [[NSString alloc] initWithFormat:@"document.getElementsByTagName('body')[0].style.webkitTextSizeAdjust= '%f%%'", zoom];

            [self executeGlobalJavascript:jsString];

            [self sendPluginSuccess:command];

        }@catch (NSException *exception) {
            [self handlePluginExceptionWithContext:exception :command];
        }
    }];
}

- (void) getAvailableMailClients:(CDVInvokedUrlCommand*)command {

    dispatch_async(dispatch_get_main_queue(), ^{

        @try {

            NSArray<NSDictionary *>* mailList = [self getMailList];
            NSMutableArray<NSDictionary *>* availableMailList = [[NSMutableArray alloc] initWithCapacity:[mailList count]];

            for (NSDictionary *mailListItem in mailList) {
                NSString *scheme = mailListItem[@"package"];

                if (scheme) {
                    if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:[scheme stringByAppendingString:@":"]]]) {
                        [availableMailList addObject:mailListItem];
                    }
                }

            }

            [self sendPluginArrayResult:availableMailList command:command];

        }@catch (NSException *exception) {
            [self handlePluginExceptionWithContext:exception :command];
        }

     });

}

- (void) openEmailApp:(CDVInvokedUrlCommand*)command {

    @try {

        if ([command.arguments count] == 1) {
            NSString* scheme = [command.arguments objectAtIndex:0];

            if (![self isNotNull:scheme]) {
                [self sendPluginErrorWithMessage:@"invalid scheme" command:command];
                return;
            }

            [[UIApplication sharedApplication] openURL:[NSURL URLWithString: [scheme stringByAppendingString:@":"]] options:@{} completionHandler:^(BOOL success) {
                if (success) {
                    [self sendPluginSuccess:command];
                }else{
                    [self sendPluginErrorWithMessage:[NSString stringWithFormat:@"%@ %@", scheme, @"does not exist"] command:command];
                }
            }];

//            if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:scheme]]) {
//                [[UIApplication sharedApplication] openURL:[NSURL URLWithString:scheme] options:@{} completionHandler:nil];
//            } else {
//                [self sendPluginErrorWithMessage:[NSString stringWithFormat:@"%@ %@", scheme, @"does not exist"] command:command];
//                return;
//            }

        } else {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:@"mailto:"] options:@{} completionHandler:nil];
            [self sendPluginSuccess:command];
        }

    }@catch (NSException *exception) {
        [self handlePluginExceptionWithContext:exception :command];
    }
}

- (void)startNetworkInfoNotifier:(CDVInvokedUrlCommand *)command {

    [self.commandDelegate runInBackground:^{
        @try {

            if ([command.arguments count] != 1) {
                [self sendPluginErrorWithMessage:@"Invalid arguments" command:command];
                return;
            }

            NSString* urlString = [command.arguments objectAtIndex:0];
            NSURL *url = [NSURL URLWithString:urlString];

            if (![self isValidUrl:url]) {
                [self sendPluginErrorWithMessage:@"Invalid url" command:command];
                return;
            }

            if (self.networkInfoCallbackId != NULL || self.reachabilityManager != NULL) {
                [self sendPluginErrorWithMessage:@"Already started" command:command];
                return;
            }
            self.networkInfoCallbackId = command.callbackId;
            __weak SystemPlugin* weakSelf = self;
            self.reachabilityManager = [[ReachabilityManager alloc] init];

            [self.reachabilityManager start:url :^(NetworkInfo * _Nonnull info) {
                [weakSelf sendNetworkInfo: info];
            }];

        }@catch (NSException *exception) {
            if (self.networkInfoCallbackId != NULL) {
                [self sendPluginErrorAndKeepCallback:exception callbackId:self.networkInfoCallbackId];
            } else {
                [self handlePluginExceptionWithContext:exception :command];
            }
        }
    }];

}

- (void)stopNetworkInfoNotifier:(CDVInvokedUrlCommand *)command {

    [self.commandDelegate runInBackground:^{
        @try {

            self.networkInfoCallbackId = NULL;
            if (self.reachabilityManager != NULL) {
                [self.reachabilityManager stop];
                self.reachabilityManager = NULL;
            }
            [self sendPluginSuccess:command];

        }@catch (NSException *exception) {
            [self handlePluginExceptionWithContext:exception :command];
        }
    }];
}

- (void)sendNetworkInfo:(NetworkInfo * _Nonnull)info {
    @try {
        if (self.networkInfoCallbackId != NULL) {

            NSDictionary *result = [self convertNetworkInfo:info];
            [self sendPluginDictionaryResultAndKeepCallback:result callbackId:self.networkInfoCallbackId];
        }
    }@catch (NSException *exception) {
        if (self.networkInfoCallbackId != NULL) {
            [self sendPluginErrorAndKeepCallback:exception callbackId:self.networkInfoCallbackId];
        } else {
            [self handlePluginExceptionWithoutContext:exception];
        }
    }
}

- (void) enableScreenProtection:(CDVInvokedUrlCommand*)command {

    __weak SystemPlugin* weakSelf = self;

    dispatch_async(dispatch_get_main_queue(), ^{

        @try {

            if (weakSelf.securityView != nil) {
                [weakSelf sendPluginErrorWithMessage:@"already enabled" command:command];
                return;
            }

            UIViewController* rootController = [UIApplication sharedApplication].delegate.window.rootViewController;
            UIView *topView = rootController.view;

            UITextField * guardTextField = [UITextField new];
            guardTextField.backgroundColor = UIColor.blackColor;
            [guardTextField setTag:INT_MAX];
            [guardTextField setUserInteractionEnabled:NO];
            [guardTextField setSecureTextEntry:YES];

            [weakSelf.webView addSubview:guardTextField];
            [weakSelf.webView sendSubviewToBack:guardTextField];

            weakSelf.securityView = guardTextField;
            [weakSelf.webView.layer.superlayer addSublayer:weakSelf.securityView.layer];
            [[weakSelf.securityView.layer.sublayers firstObject] addSublayer:weakSelf.webView.layer];

            [topView setNeedsLayout];

            [weakSelf sendPluginSuccess:command];
        }@catch (NSException *exception) {
            [weakSelf handlePluginExceptionWithContext:exception :command];
        }

     });
}

- (void) disableScreenProtection:(CDVInvokedUrlCommand*)command {

    __weak SystemPlugin* weakSelf = self;

    dispatch_async(dispatch_get_main_queue(), ^{

        @try {

            if (weakSelf.securityView == nil) {
                [weakSelf sendPluginErrorWithMessage:@"already disabled" command:command];
                return;
            }

            UIViewController* rootController = [UIApplication sharedApplication].delegate.window.rootViewController;
            UIView *topView = rootController.view;

            [weakSelf.securityView removeFromSuperview];
            weakSelf.securityView = nil;
            [topView insertSubview:weakSelf.webView atIndex:1];
            [topView setNeedsLayout];

            [weakSelf sendPluginSuccess:command];
        }@catch (NSException *exception) {
            [weakSelf handlePluginExceptionWithContext:exception :command];
        }

     });
}


#pragma mark - utility functions

- (NSArray<NSDictionary *>*) getMailList
{
    return @[
        @{@"label": @"Airmail", @"package": @"airmail"},
        @{@"label": @"Apple Mail", @"package": @"com.apple.mobilemail"},
        @{@"label": @"Blue mail", @"package": @"bluemail"},
        @{@"label": @"Canary Mail", @"package": @"canary"},
        @{@"label": @"Edison Mail", @"package": @"edisonmail"},
        @{@"label": @"Gmail", @"package": @"googlegmail"},
        @{@"label": @"Hey", @"package": @"hey"},
        @{@"label": @"Mail.ru", @"package": @"mailrumail"},
        @{@"label": @"myMail", @"package": @"mycom-mail-x-callback"},
        @{@"label": @"Newton Mail", @"package": @"cloudmagic"},
        @{@"label": @"Outlook", @"package": @"ms-outlook"},
        @{@"label": @"Polymail", @"package": @"polymail"},
        @{@"label": @"ProtonMail", @"package": @"protonmail"},
        @{@"label": @"Spark", @"package": @"readdlespark"},
        @{@"label": @"Spike", @"package": @"spike"},
        @{@"label": @"Twobird", @"package": @"twobird"},
        @{@"label": @"Yahoo Mail", @"package": @"ymail"},
        @{@"label": @"Yandex.Mail", @"package": @"yandexmail"}
    ];
}


- (NSDictionary*)convertNetworkInfo:(NetworkInfo * _Nonnull)info {
    NSMutableDictionary* result = [[NSMutableDictionary alloc] init];

    if (info.addresses) {
        NSMutableDictionary *addressDict = [[NSMutableDictionary alloc] init];
        for (NetworkAddress *address in info.addresses) {
            NSArray *ips = [addressDict objectForKey:address.interface];
            if (ips) {
                NSArray *updatedIps = [[NSArray alloc] initWithArray:ips];
                [addressDict setValue:[updatedIps arrayByAddingObject:address.ip] forKey:address.interface];
            } else {
                [addressDict setValue:@[address.ip] forKey:address.interface];
            }
        }
        [result setValue:addressDict forKey:@"addresses"];
    }

    if (info.proxy) {
        NSMutableArray *proxyList = [[NSMutableArray alloc] init];
        for (ProxyInfo *proxy in info.proxy) {
            NSMutableDictionary *proxyDictionary = [[NSMutableDictionary alloc] init];
            [proxyDictionary setValue:proxy.type forKey:@"type"];
            if (proxy.host) {
                [proxyDictionary setValue:proxy.host forKey:@"host"];
            }
            if (proxy.port) {
                [proxyDictionary setValue:proxy.port forKey:@"port"];
            }
            if (proxy.urlKey) {
                [proxyDictionary setValue:proxy.urlKey forKey:@"urlKey"];
            }
            [proxyList addObject: proxyDictionary];
        }

        [result setValue:proxyList forKey:@"proxy"];
    }

    if (info.transport) {
        NSArray<NSString*>* transportList = @[info.transport];
        [result setValue:transportList forKey:@"transport"];
    }

    return result;

}

- (BOOL) isValidUrl: (NSURL*)url
{
    return  url && url.host && url.scheme;
}

- (BOOL) isNotNull: (NSString*)str
{
    return str != nil && str.length != 0 && ![str isEqual: @"<null>"];
}

- (void) sendPluginSuccess:(CDVInvokedUrlCommand*)command{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) sendPluginArrayResult:(NSArray*)result command:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) sendPluginErrorWithMessage:(NSString*)errorMessage command:(CDVInvokedUrlCommand*)command
{
    [self _logError:errorMessage];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) handlePluginExceptionWithContext: (NSException*) exception :(CDVInvokedUrlCommand*)command
{
    [self _logError:[NSString stringWithFormat:@"EXCEPTION: %@", exception.reason]];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) sendPluginErrorAndKeepCallback: (NSException*) exception callbackId:(NSString*)callbackId
{
    [self _logError:[NSString stringWithFormat:@"EXCEPTION: %@", exception.reason]];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) sendPluginDictionaryResultAndKeepCallback:(NSDictionary*)result callbackId:(NSString*)callbackId {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) handlePluginExceptionWithoutContext: (NSException*) exception
{
    [self _logError:[NSString stringWithFormat:@"EXCEPTION: %@", exception.reason]];
}

- (void)_logError: (NSString*)msg
{
    NSLog(@"%@ ERROR: %@", LOG_TAG, msg);
    NSString* jsString = [NSString stringWithFormat:@"console.error(\"%@: %@\")", LOG_TAG, [self escapeJavascriptString:msg]];
    [self executeGlobalJavascript:jsString];
}

- (NSString*)escapeJavascriptString: (NSString*)str
{
    NSString* result = [str stringByReplacingOccurrencesOfString: @"\\\"" withString: @"\""];
    result = [result stringByReplacingOccurrencesOfString: @"\"" withString: @"\\\""];
    result = [result stringByReplacingOccurrencesOfString: @"\n" withString: @"\\\n"];
    return result;
}

- (void)executeGlobalJavascript: (NSString*)jsString
{
    [self.commandDelegate evalJs:jsString];
}

@end
