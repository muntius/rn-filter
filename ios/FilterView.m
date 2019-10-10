//
//  View.m
//  test
//
//  Created by Bachir Khiati on 24/09/2019.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "React/RCTBridge.h"
#import "React/RCTEventDispatcher.h"
#import "FilterView.h"
#import "ResizeUtils.h"
@import GLKit;
@import OpenGLES;

@implementation FilterView  {
    RCTEventDispatcher *_eventDispatcher;
    
    UIImage *image;
    UIImage *editImage;
    UIImage *fullsizeImage;
    UIImage *thumbnailImage;
    
    UIImageView *imageView;
    UITextView *textView;
    
    CIImage * editImageCGImage;
    CIImage * editThumbCGImage;
    CIImage *resultImage;
    
    EAGLContext *_eaglContext;
    CIContext *_cictx;
    GLKView *_viewForImage;
    
    BOOL isEditing;
    BOOL SatFilter;
    BOOL BritFilter;
    BOOL ContFilter;
    BOOL previewOriginal;
    
    BOOL BlurFilter;
    BOOL VignetteFilter;
    CALayer *filterLayer;
    
    CGRect RectImageView;
    CGRect glkViewRect;
    
    NSNumber *inputSaturationDefault;
    NSNumber *inputBrightnessDefault;
    NSNumber *inputContrastDefault;
    NSNumber *inputBlurDefault;
    NSNumber *inputVignetteDefault;
    
    NSNumber *inputSaturation;
    NSNumber *inputBrightness;
    NSNumber *inputContrast;
    NSNumber *inputBlur;
    NSNumber *inputVignette;
    
    NSArray *CIFilterNames;
    NSArray *filterNames;
    
    
}



- (instancetype)init
{
    if ((self = [super init])) {
        inputSaturationDefault = @1;
        inputBrightnessDefault = @0;
        inputContrastDefault = @1;
        inputVignetteDefault  = @0;
        inputBlurDefault = @0;
        
        inputSaturation = @1;
        inputBrightness = @0;
        inputContrast = @1;
        inputVignette  = @1;
        inputBlur = @0;
    }
    
    return self;
}

-(void)calculateRects
{
    self->RectImageView = CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
    self->glkViewRect = CGRectMake(0, 0, self.frame.size.width,self.frame.size.height);
}


-(void)configureImage:(NSString*)imageUrl
{
    NSData * imageData = [[NSData alloc] initWithContentsOfURL: [NSURL URLWithString: imageUrl]];
    UIImage *image = [UIImage imageWithData: imageData];
    self->image = image;
    UIImage *resizedImage;
    
    if (image.size.width > image.size.height) {
        resizedImage = [image resizedImageByWidth:(int)self.frame.size.width];
    }
    else
    {
        resizedImage = [image resizedImageByHeight:(int)self.frame.size.width];
    }
    NSString *resizeString = [NSString stringWithFormat:@"%ix%i#",(int)self.frame.size.width,(int)self.frame.size.width];
    UIImage *editResizedImage = [image resizedImage:resizeString];
    self->editImage = editResizedImage;
    
}

-(void)setupGLContext
{
    _eaglContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    _viewForImage = [[GLKView alloc] initWithFrame:glkViewRect context:_eaglContext];
    _cictx = [CIContext contextWithEAGLContext:_eaglContext options:@{kCIContextWorkingColorSpace: [NSNull null]}];
    
    [_viewForImage setEnableSetNeedsDisplay:YES];
    
    [self addSubview:_viewForImage];
    
    [self loadImageIntoEAGLContext];
    
    _viewForImage.alpha = 1;
    [_viewForImage setHidden:NO];
}

-(void)loadImageIntoEAGLContext
{
    CIImage *image = [CIImage imageWithCGImage:[self->editImage CGImage]];
    self-> editImageCGImage = image;
    
    CIImage *thumbnail = [CIImage imageWithCGImage:[self->thumbnailImage CGImage]];
    self-> editThumbCGImage = thumbnail;
    
    [_viewForImage bindDrawable];
    
    [_cictx drawImage:image inRect:CGRectMake(0.0, 0.0,_viewForImage.drawableWidth,_viewForImage.drawableHeight) fromRect:[image extent]];
    
    [_viewForImage display];
    
}




- (void)layoutSubviews
{
    imageView.frame = self.bounds;
    [self calculateRects];
    [self configureImage:_src];
    [self setupGLContext];
    
    //   [self UpdateColorInputs];
}


-(void)UpdateColorInputs:(NSString *)filterName
{
    
    CIFilter *filter;
    CIImage *currentImage = editImageCGImage;
    if(!previewOriginal){
        if(SatFilter || BritFilter || ContFilter){
            filter = [CIFilter filterWithName: @"CIColorControls"
                          withInputParameters: @{
                                                 @"inputImage"      : editImageCGImage,
                                                 @"inputSaturation" : inputSaturation,
                                                 @"inputBrightness" : inputBrightness,
                                                 @"inputContrast"   : inputContrast
                                                 }];
            currentImage = [filter valueForKey:kCIOutputImageKey];
        }
        if(BlurFilter){
            NSLog(@"%@", inputBlur);
            filter = [CIFilter filterWithName: @"CIGaussianBlur"
                          withInputParameters: @{
                                                 @"inputImage"      : currentImage,
                                                 @"inputRadius" : inputBlur
                                                 }];
            currentImage = [filter valueForKey:kCIOutputImageKey];
        }
        if(VignetteFilter){
            filter = [CIFilter filterWithName: @"CIVignette"
                          withInputParameters: @{
                                                 @"inputImage"      : currentImage,
                                                 @"inputIntensity" : @1,
                                                 @"inputRadius"   : inputVignette
                                                 }];
            currentImage = [filter valueForKey:kCIOutputImageKey];
        }
        
    }
    
    [_viewForImage bindDrawable];
    if (_eaglContext != [EAGLContext currentContext]) {
        [EAGLContext setCurrentContext:_eaglContext];
    }
    
    
    glClearColor(0.0, 0.0, 0.0, 0.0);
    glClear(GL_COLOR_BUFFER_BIT);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    CGRect extentRect = [editImageCGImage extent];
    if (CGRectIsInfinite(extentRect) || CGRectIsEmpty(extentRect)) {
        extentRect = _viewForImage.bounds;
    }
    [_cictx drawImage:currentImage inRect:CGRectMake(0.0, 0.0,_viewForImage.drawableWidth,_viewForImage.drawableHeight) fromRect:extentRect];
    [_viewForImage display];
}

-(void) setSaturation:(NSNumber *)value{
    inputSaturation = value;
    if(![value isEqualToNumber:inputSaturationDefault]){
        
        SatFilter= true;
    } else {
        SatFilter= false;
    }
    [self UpdateColorInputs:@"SBC"];
}


-(void) setBrightness:(NSNumber *)value{
    inputBrightness = value;
    if(![value isEqualToNumber:inputBrightnessDefault]){
        
        BritFilter= true;
    } else {
        BritFilter= false;
    }
    
    [self UpdateColorInputs:@"SBC"];
}
-(void) setContrast:(NSNumber *)value{
    inputContrast = value;
    if(![value isEqualToNumber:inputContrastDefault]){
        ContFilter= true;
    } else {
        ContFilter= false;
    }
    [self UpdateColorInputs:@"SBC"];
}

-(void) setBlur:(NSNumber *)value{
    inputBlur = value;
    
    if(![value isEqualToNumber:inputBlurDefault]){
        BlurFilter = true;
    } else {
        BlurFilter = false;
    }
    [self UpdateColorInputs:@"Blur"];
}

-(void) setVignette:(NSNumber *)value{
    inputVignette = value;
    if(![value isEqualToNumber:inputVignetteDefault]){
        VignetteFilter = true;
    } else {
        VignetteFilter = false;
    }
    [self UpdateColorInputs:@"Vignette"];
}

-(void) setOriginal:(BOOL *)value{
    previewOriginal = value;
    
    [self UpdateColorInputs:@"previewOriginal"];
}

-(void) setReset{
    previewOriginal = BritFilter = SatFilter = ContFilter = BlurFilter = VignetteFilter = false;
    inputBlur = inputBlurDefault;
    inputSaturation = inputSaturationDefault;
    inputContrast = inputContrastDefault;
    inputBrightness = inputBrightnessDefault;
    inputVignette = inputVignetteDefault;
    [self UpdateColorInputs:@"Original"];
}



-(void) takeShot: (NSNumber *)width height:(NSNumber *)height {
    UIGraphicsBeginImageContextWithOptions(_viewForImage.bounds.size, YES, 0);
    [_viewForImage drawViewHierarchyInRect:_viewForImage.bounds afterScreenUpdates:YES];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    CGSize newSize = CGSizeMake([height doubleValue], [width doubleValue]);
    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0,0,newSize.width,newSize.height)];
    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    NSURL *tmpDirURL = [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
    NSURL *fileURL = [[tmpDirURL URLByAppendingPathComponent:[[NSUUID UUID] UUIDString]] URLByAppendingPathExtension:@"jpeg"];
    NSData *imageData = UIImageJPEGRepresentation(newImage, 1);
    [imageData writeToFile:[fileURL path] atomically:YES];
    if (!self.onDataReturned) {
        return;
    }
    self.onDataReturned(@{
                          
                          @"url": [fileURL path]
                          });
}


@end


//##########################
//##########################
//##########################
//##########################
//##########################
//##########################
//##########################

