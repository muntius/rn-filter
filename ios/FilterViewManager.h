#import <React/RCTViewManager.h>
#import "FilterView.h"

@interface FilterViewManager : RCTViewManager

-(void) setSaturation:(nonnull NSNumber*) reactTag
      value:(nonnull NSNumber*) value;
-(void) setBrightness:(nonnull NSNumber*) reactTag
                value:(nonnull NSNumber*) value;
-(void) setContrast:(nonnull NSNumber*) reactTag
                value:(nonnull NSNumber*) value;

@end
