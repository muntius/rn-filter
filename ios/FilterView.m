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
    
    EAGLContext *_eaglContext;
    CIContext *_cictx;
    GLKView *_viewForImage;
    
    BOOL isEditing;
    CALayer *filterLayer;
    
    CGRect RectImageView;
    CGRect glkViewRect;
    
    NSNumber *inputSaturation;
    NSNumber *inputBrightness;
    NSNumber *inputContrast;
    
    
    NSArray *CIFilterNames;
    NSArray *filterNames;
    
    
}



- (instancetype)init
{
    if ((self = [super init])) {
        inputSaturation = @1;
        inputBrightness = @0;
        inputContrast = @1;
        CIFilterNames = @[
                          @"CIPhotoEffectChrome",
                          @"CIPhotoEffectFade",
                          @"CIPhotoEffectInstant",
                          @"CIPhotoEffectNoir",
                          @"CIPhotoEffectProcess",
                          @"CIPhotoEffectTonal",
                          @"CIPhotoEffectTransfer",
                          @"CISepiaTone"
                          ];
        filterNames = @[
                        @"Chrome",
                        @"Fade",
                        @"Instant",
                        @"Noir",
                        @"Process",
                        @"Tonal",
                        @"Transfer",
                        @"Sepia"
                        ];
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
        NSLog(@"Wider");
        resizedImage = [image resizedImageByWidth:(int)self.frame.size.width];
    }
    else
    {
        //NSLog(@"Taller");
        resizedImage = [image resizedImageByHeight:(int)self.frame.size.width];
    }
    NSString *resizeString = [NSString stringWithFormat:@"%ix%i#",(int)self.frame.size.width,(int)self.frame.size.width];
    UIImage *editResizedImage = [image resizedImage:resizeString];
    self->editImage = editResizedImage;
    [self thumbnailWithContentsOfURL: imageUrl  maxPixelSize:(CGFloat) 100];
    
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
    
    //   [self UpdateInputParameters];
}


-(void)UpdateInputParameters
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

//Photo effect Fade
//Photo effect Instant
//Photo effect Mono
//Photo effect Noir
//MonoChrome 1 
//Posterize 1 




-(void) setPredifinedFilter:(NSNumber*)filterId
{
    CIFilter *  filter = [CIFilter filterWithName: [CIFilterNames objectAtIndex:filterId.integerValue]
                              withInputParameters: @{
                                                     @"inputImage"      : editImageCGImage
                                                     }];
    [filter setDefaults];
    CIImage *image = [CIImage imageWithCGImage:[self->editImage CGImage]];
    CIImage *resultImage = [filter valueForKey:kCIOutputImageKey];
    [_viewForImage bindDrawable];
    if (_eaglContext != [EAGLContext currentContext]) {
        [EAGLContext setCurrentContext:_eaglContext];
    }
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


-(void) generateThumbsFilter
{
    //-(void) generateThumbsFilter
    CIContext *context = [CIContext contextWithOptions:nil];
    NSString *path = NSTemporaryDirectory();
    
    
    NSMutableArray *replacementArray = [NSMutableArray arrayWithCapacity:[CIFilterNames count]];
    [CIFilterNames enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        //    NSMutableArray *replacementArray = [NSMutableArray arrayWithCapacity:[CIFilterNames count]];
        CIFilter * filter = [CIFilter filterWithName: obj
                                 withInputParameters: @{
                                                        @"inputImage"      : editThumbCGImage
                                                        }];
        [filter setDefaults];
        CIImage *finalImage = [filter valueForKey:kCIOutputImageKey];
        CGImageRef img = [context createCGImage:finalImage fromRect:[finalImage extent]];
        UIImage *thumbnailResult = [[UIImage alloc] initWithCGImage:img];
        NSString *filePath = [path stringByAppendingPathComponent: [NSString stringWithFormat:@"%@%@", [[NSUUID UUID] UUIDString] , @".jpeg"]]  ;
        NSData *dataForJPEGFile = UIImageJPEGRepresentation(thumbnailResult, 1.0);
        NSError *error2 = nil;
        if (![dataForJPEGFile writeToFile:filePath options:NSAtomicWrite error:&error2])
        {
            NSLog(@"%s", "Errrorrrrr");
            NSLog(@"%@", error2);
        }
        CGImageRelease(img);
        
        [replacementArray addObject:@{@"id": @(idx).stringValue,
                                      @"uri": filePath,
                                      @"name": [filterNames objectAtIndex:idx]
                                      }];
    }];
    _onThumbsReturned(@{ @"thumbs": replacementArray });
    
    
    
}

- (void)thumbnailWithContentsOfURL:(NSString *)source maxPixelSize:(CGFloat)maxPixelSize
{
    NSURL *URL = [NSURL URLWithString:source];
    
    CGImageSourceRef imageSource = CGImageSourceCreateWithURL((__bridge CFURLRef)URL, NULL);
    NSAssert(imageSource != NULL, @"cannot create image source");
    
    NSDictionary *imageOptions = @{
                                   (NSString const *)kCGImageSourceCreateThumbnailFromImageIfAbsent : (NSNumber const *)kCFBooleanTrue,
                                   (NSString const *)kCGImageSourceThumbnailMaxPixelSize            : @(maxPixelSize),
                                   (NSString const *)kCGImageSourceCreateThumbnailWithTransform     : (NSNumber const *)kCFBooleanTrue
                                   };
    CGImageRef thumbnail = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, (__bridge CFDictionaryRef)imageOptions);
    CFRelease(imageSource);
    UIImage *result = [[UIImage alloc] initWithCGImage:thumbnail];
    CGImageRelease(thumbnail);
    self->thumbnailImage = result;
}

-(void) setSaturation:(NSNumber *)value{
    inputSaturation = value;
    [self UpdateInputParameters];
}
-(void) setBrightness:(NSNumber *)value{
    inputBrightness = value;
    [self UpdateInputParameters];
}
-(void) setContrast:(NSNumber *)value{
    inputContrast = value;
    [self UpdateInputParameters];
}

-(void) setFilter:(nonnull NSNumber *)value{
    [self setPredifinedFilter: value ];
    
}

-(void) generateFilters{
    [self generateThumbsFilter];
}


@end


//##########################
//##########################
//##########################
//##########################
//##########################
//##########################
//##########################

