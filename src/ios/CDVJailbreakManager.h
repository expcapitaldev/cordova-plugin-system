#import "Foundation/Foundation.h"
#import <sys/stat.h>
#import <sys/sysctl.h>

@interface JailbreakManager : NSObject
- (BOOL)isJailbroken;
@end

