#import "CDVSystemPlugin.h"

static NSString*const LOG_TAG = @"SystemPlugin[native]";

//@interface MailClient : NSObject
//@property(nonatomic, strong) NSString *title;
//@property(nonatomic, strong) NSString *scheme;
//@end
//
//@implementation MailClient
//@end

@interface SystemPlugin () {}
@end

@implementation SystemPlugin

// @override abstract
- (void)pluginInitialize {
    NSLog(@"Starting SystemPlugin");
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
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

//- (void) handlePluginExceptionWithoutContext: (NSException*) exception
//{
//    [self _logError:[NSString stringWithFormat:@"EXCEPTION: %@", exception.reason]];
//}

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
