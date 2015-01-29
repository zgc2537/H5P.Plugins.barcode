/**
 * Copyright 2009 Jeff Verkoeyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <UIKit/UIKit.h>
#include <AudioToolbox/AudioToolbox.h>
#import <AVFoundation/AVFoundation.h>
#include "Decoder.h"
#include "parsedResults/ParsedResult.h"
#include "OverlayView.h"
#import "CBarcodeFormat.h"

@protocol ZXingDelegate;

#if !TARGET_IPHONE_SIMULATOR
#define HAS_AVFF 1
#endif

@interface ZXingWidget : UIView<DecoderDelegate,
UINavigationControllerDelegate
#if HAS_AVFF
, AVCaptureVideoDataOutputSampleBufferDelegate
#endif
> {
  NSSet *readers;
  ParsedResult *result;
  OverlayView *overlayView;
  SystemSoundID beepSound;
  NSURL *soundToPlay;
  id<ZXingDelegate> delegate;
  BOOL wasCancelled;
  BOOL oneDMode;
#if HAS_AVFF
  AVCaptureSession *captureSession;
  AVCaptureVideoPreviewLayer *prevLayer;
#endif
  BOOL decoding;
}

#if HAS_AVFF
@property (nonatomic, retain) AVCaptureSession *captureSession;
@property (nonatomic, retain) AVCaptureVideoPreviewLayer *prevLayer;
#endif
@property (nonatomic, retain ) NSSet *readers;
@property (nonatomic, assign) id<ZXingDelegate> delegate;
@property (nonatomic, retain) NSURL *soundToPlay;
@property (nonatomic, retain) ParsedResult *result;
@property (nonatomic, retain) OverlayView *overlayView;

- (id)initWithDelegate:(id<ZXingDelegate>)scanDelegate
             withFrame:(CGRect)frame OneDMode:(BOOL)shouldUseoOneDMode;
- (BOOL)fixedFocus;
- (void)setTorch:(BOOL)status;
- (BOOL)torchIsOn;
- (void)pauseScan;
- (void)resumeScan;
@end

@protocol ZXingDelegate
@optional
- (void)zxingController:(ZXingWidget*)controller
        withDecodeImage:(UIImage*)dImg
          didScanResult:(NSString *)result
          didScanFormat:(BarcodeFormat)format;
- (void)zxingControllerDidCancel:(ZXingWidget*)controller;
@end
