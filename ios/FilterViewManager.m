//
//  Filter.m
//  test
//
//  Created by Bachir Khiati on 24/09/2019.
//  Copyright © 2019 Facebook. All rights reserved.
//
//#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <React/RCTViewManager.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTUIManager.h>

#import "FilterViewManager.h"
#import "FilterView.h"

@implementation FilterViewManager
@synthesize bridge = _bridge;

RCT_EXPORT_MODULE(RNFilter);

RCT_EXPORT_VIEW_PROPERTY(src, NSString);

//RCT_EXPORT_VIEW_PROPERTY(onSurfaceCreate, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onThumbsReturned, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDataReturned, RCTBubblingEventBlock)


- (UIView *)view
{
  FilterView *filterView = [[FilterView alloc] init];
  filterView.manager = self;
  return filterView;
}

RCT_EXPORT_METHOD(
                  setSaturation:(nonnull NSNumber*) reactTag
                  value:(nonnull NSNumber*) value ) {

  [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    FilterView *view = (FilterView *)viewRegistry[reactTag];
    if (!view || ![view isKindOfClass:[FilterView class]]) {
      RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
      return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
      [view setSaturation:value];
    });
  }];
  
}

RCT_EXPORT_METHOD(
                  setBrightness:(nonnull NSNumber*) reactTag
                  value:(nonnull NSNumber*) value ) {
  [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    FilterView *view = (FilterView *)viewRegistry[reactTag];
    if (!view || ![view isKindOfClass:[FilterView class]]) {
      RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
      return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
      [view setBrightness:value];
    });
  }];
  
  
}

RCT_EXPORT_METHOD(
                  setContrast:(nonnull NSNumber*) reactTag
                  value:(nonnull NSNumber*) value ) {
  
  [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    FilterView *view = (FilterView *)viewRegistry[reactTag];
    if (!view || ![view isKindOfClass:[FilterView class]]) {
      RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
      return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
      [view setContrast:value];
    });
  }];
 
}


RCT_EXPORT_METHOD(
                  setVignette:(nonnull NSNumber*) reactTag
                  value:(nonnull NSNumber*) value ) {
    
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        FilterView *view = (FilterView *)viewRegistry[reactTag];
        if (!view || ![view isKindOfClass:[FilterView class]]) {
            RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
            return;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [view setVignette:value];
        });
    }];
    
}


RCT_EXPORT_METHOD(
                  setBlur:(nonnull NSNumber*) reactTag
                  value:(nonnull NSNumber*) value ) {
    
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        FilterView *view = (FilterView *)viewRegistry[reactTag];
        if (!view || ![view isKindOfClass:[FilterView class]]) {
            RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
            return;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [view setBlur:value];
        });
    }];
    
}

RCT_EXPORT_METHOD(
                  capture:(nonnull NSNumber*) reactTag
                  width:(nonnull NSNumber*) width
                  height:(nonnull NSNumber*) height ) {
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        FilterView *view = (FilterView *)viewRegistry[reactTag];
        if (!view || ![view isKindOfClass:[FilterView class]]) {
            RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
            return;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [view takeShot:width height:height];
        });
    }];
}


- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}




@end


