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
-(void) setFilter:(nonnull NSNumber*) reactTag
              value:(nonnull NSNumber*) value;
-(void) generateFilters:(nonnull NSNumber*) reactTag;

@end
