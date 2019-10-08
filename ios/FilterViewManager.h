#import <React/RCTViewManager.h>
#import <React/RCTComponent.h>
#import "FilterView.h"

@interface FilterViewManager : RCTViewManager

-(void) setSaturation:(nonnull NSNumber*) reactTag
      value:(nonnull NSNumber*) value;
-(void) setBrightness:(nonnull NSNumber*) reactTag
                value:(nonnull NSNumber*) value;
-(void) setContrast:(nonnull NSNumber*) reactTag
                value:(nonnull NSNumber*) value;
-(void) capture:(nonnull NSNumber*) reactTag
            width:(nonnull NSNumber*) width
            height:(nonnull NSNumber*) height;

@end
