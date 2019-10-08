//
//  FilterView.h
//  test
//
//  Created by Bachir Khiati on 24/09/2019.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <React/RCTBridge.h>
#import "FilterViewManager.h"

#import "FilterView.h"

@class FilterViewManager;

@interface FilterView: UIView

@property (nonatomic, strong) FilterView *FilterView;
@property (nonatomic, strong) FilterViewManager *manager;

@property (nonatomic, copy) NSString *src;
@property (nonatomic, copy) NSNumber *contentMode;
@property (nonatomic, copy) NSNumber *value;

-(void) setSaturation:(NSNumber *)value;
-(void) setBrightness:(NSNumber *)value;
-(void) setContrast:(NSNumber *)value;
-(void) setVignette:(nonnull NSNumber *)value;
-(void) setBlur:(nonnull NSNumber *)value;
-(void) takeShot:(nonnull NSNumber *)width height:(nonnull NSNumber *)height;


//@property (nonatomic, copy) RCTDirectEventBlock onSurfaceCreate;
@property (nonatomic, copy) RCTBubblingEventBlock onDataReturned;
@property (nonatomic, copy) RCTBubblingEventBlock onThumbsReturned;

@end


