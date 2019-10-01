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
    UIImageView *imageView;
    UITextView *textView;
    
    CIImage * editImageCGImage;
    
    
    EAGLContext *_eaglContext;
    CIContext *_cictx;
    GLKView *_viewForImage;
    
    BOOL isEditing;
    CALayer *filterLayer;
    
    CGRect RectImageView;
    CGRect RectTextView;
    CGRect glkViewRect;
    CGRect controllerViewRect;
    
    NSNumber *inputSaturation;
    NSNumber *inputBrightness;
    NSNumber *inputContrast;
    
}


- (instancetype)init
{
    if ((self = [super init])) {
        inputSaturation = @1;
        inputBrightness = @0;
        inputContrast = @1;
    }
    
    return self;
}

-(void)calculateRects
{
    self->RectImageView = CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
    self->RectTextView = CGRectMake(0, 0, 22, 30);
    self->glkViewRect = CGRectMake(0, 0, self.frame.size.width,self.frame.size.width);
    self->controllerViewRect = CGRectMake(0, self.frame.size.width,self.frame.size.width , self.frame.size.height-self.frame.size.width);
}


-(void)configureImage:(NSString*)imageUrl
{
    NSData * imageData = [[NSData alloc] initWithContentsOfURL: [NSURL URLWithString: imageUrl]];
    UIImage *image = [UIImage imageWithData: imageData];
    self->image = image;
    UIImage *resizedImage;

    if (image.size.width > image.size.height) {
        NSLog(@"Wider");
        resizedImage = [image resizedImageByWidth:(int)self.frame.size.width];
    }
    else
    {
        //NSLog(@"Taller");
        resizedImage = [image resizedImageByHeight:(int)self.frame.size.width];
    }
    NSString *resizeString = [NSString stringWithFormat:@"%ix%i#",(int)self.frame.size.width,(int)self.frame.size.width];
    UIImage *editResizedImage = [image resizedImageByMagick:resizeString];
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
    
    [_viewForImage bindDrawable];
    
    [_cictx drawImage:image inRect:CGRectMake(0.0, 0.0,_viewForImage.drawableWidth,_viewForImage.drawableHeight) fromRect:[image extent]];
    
    [_viewForImage display];
    
}




- (void)layoutSubviews
{
    NSLog(@"width after");
    NSLog(@"self.editImage.size: %f",self.frame.size.width);
    NSLog(@"height");
    NSLog(@"self.editImage.size: %f",self.frame.size.height);
    imageView.frame = self.bounds;
    //  textView.frame = self.bounds;
    NSLog(@"TEst test");
    NSLog(@"%@", _src);
    [self calculateRects];
    [self configureImage:_src];
    [self setupGLContext];
    
    //   [self didUpdateInputParameters];
}


-(void)didUpdateInputParameters
{
    CIFilter *filter = [CIFilter filterWithName: @"CIColorControls"
                            withInputParameters: @{
                                                   @"inputImage"      : editImageCGImage,
                                                   @"inputSaturation" : inputSaturation,
                                                   @"inputBrightness" : inputBrightness,
                                                   @"inputContrast"   : inputContrast
                                                   }];
    CIImage *image = [CIImage imageWithCGImage:[self->editImage CGImage]];
    
    CIImage *resultImage = [filter valueForKey:kCIOutputImageKey];
    
    [_viewForImage bindDrawable];
    
    if (_eaglContext != [EAGLContext currentContext]) {
        [EAGLContext setCurrentContext:_eaglContext];
    }
    //glClearColor(0.5, 0.5, 0.5, 1.0);
    
    glClearColor(0.0, 0.0, 0.0, 0.0);
    
    glClear(GL_COLOR_BUFFER_BIT);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    
    CGRect extentRect = [image extent];
    
    if (CGRectIsInfinite(extentRect) || CGRectIsEmpty(extentRect)) {
        extentRect = _viewForImage.bounds;
    }
    
    [_cictx drawImage:resultImage inRect:CGRectMake(0.0, 0.0,_viewForImage.drawableWidth,_viewForImage.drawableHeight) fromRect:extentRect];
    [_viewForImage display];
    
}

-(void) setSaturation:(NSNumber *)value{
    inputSaturation = value;
    [self didUpdateInputParameters];
}
-(void) setBrightness:(NSNumber *)value{
    inputBrightness = value;
    [self didUpdateInputParameters];
}
-(void) setContrast:(NSNumber *)value{
    inputContrast = value;
    [self didUpdateInputParameters];
}


@end

