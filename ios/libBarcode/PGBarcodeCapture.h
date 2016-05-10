//
//  PGBarcodeCapture.h
//  libBarcode
//
//  Created by DCloud on 15/12/9.
//  Copyright © 2015年 DCloud. All rights reserved.
//

#import "ZXCapture.h"
#import "ZXResult.h"
#import "PGBarcodeDef.h"


typedef ZXResult PGBarcodeResult;
typedef ZXCapture PGBarcodeCapture;
typedef ZXDecodeHints PGBarcodeHints;
//
@interface ZXCapture(PGBarcode)
- (void) setVideoOrientation:(AVCaptureVideoOrientation)videoOrientation;
+ (PGBarcodeResult*)decodeWithCGImage:(CGImageRef)imageToDecode
                            withHints:(PGBarcodeHints*)hints error:(NSError**)error;
+ (PGBarcodeHints*)decodeHintsWithFilters:(NSArray*)filters;
@end

@interface ZXResult(PGBarcode)
-(PGBarcodeFormat)scanBarcodeFormat;
+ (ZXBarcodeFormat)H5PForamt2ZX:(PGBarcodeFormat)h5pFormat;
+ (PGBarcodeFormat)ZXForamt2H5P:(ZXBarcodeFormat)zxFormat;
@end

