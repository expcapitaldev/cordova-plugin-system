#import "CDVSystemPlugin.h"

static NSString*const LOG_TAG = @"SystemPlugin[native]";

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

#pragma mark - utility functions

- (void) sendPluginSuccess:(CDVInvokedUrlCommand*)command{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

//- (void) sendPluginErrorWithMessage:(NSString*)errorMessage command:(CDVInvokedUrlCommand*)command
//{
//    [self _logError:errorMessage];
//    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
//    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
//}

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
