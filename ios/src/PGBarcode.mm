/*
 *------------------------------------------------------------------
 *  pandora/feature/PGShare
 *  Description:
 *    上传插件实现定义
 *      负责和js层代码交互，js native层对象维护
 *  DCloud Confidential Proprietary
 *  Copyright (c) Department of Research and Development/Beijing/DCloud.
 *  All Rights Reserved.
 *
 *  Changelog:
 *	number	author	modify date modify record
 *   0       xty     2013-03-22 创建文件
 *------------------------------------------------------------------
 */
#import "PGBarcode.h"
#import <QRCodeReader.h>
#import "TwoDDecoderResult.h"
#import "Decoder.h"
#import "QRCodeReader.h"
#import "PDRCoreFeature.h"
#import "PTLog.h"
#import "PDRToolSystemEx.h"
#import "PDRCoreWindowManager.h"
#import "PDRCommonString.h"
#import "PDRCoreAppFrame.h"
#import "libBarcode/zxing/iphone/ZXingWidget/Classes/MultiFormatOneDReader.h"
#import "libBarcode/zxing/iphone/ZXingWidget/Classes/MultiFormatUPCEANReader.h"
#import "libBarcode/zxing/iphone/ZXingWidget/Classes/AztecReader.h"
#import "libBarcode/zxing/iphone/ZXingWidget/Classes/MultiFormatUPCEANReader.h"
#import "libBarcode/zxing/iphone/ZXingWidget/Classes/DataMatrixReader.h"

@class PTPathUtil;
@interface DHDecode : Decoder
@property(nonatomic, retain)NSString *decImgPath;
@property(nonatomic, retain)NSString *jsCBId;
@end

@implementation DHDecode

@synthesize jsCBId;
@synthesize decImgPath;

- (void)dealloc {
    self.jsCBId = nil;
    self.decImgPath = nil;
    [super dealloc];
}

@end

@implementation PGBarcode

@synthesize callBackID;
@synthesize scaning;
@synthesize decodeImgWToFile;
@synthesize decodeImgPath;

- (void) onAppEnterBackground {
    if ( _widget ) {
      //  [_widget removeFromSuperview];
    }
}

- (void) onAppEnterForeground {
    if (_widget ) {
      //  [self.JSFrameContext.webView.scrollView addSubview:_widget];
        if ( self.scaning ) {
            [_widget resumeScan];
        }
    }
}

- (void)resize:(PGMethod*)command {
    if ( _widget ) {
        NSArray *size = [command.arguments objectAtIndex:0];
        CGFloat left = [[size objectAtIndex:0] floatValue];
        CGFloat top = [[size objectAtIndex:1] floatValue];
        CGFloat width = [[size objectAtIndex:2] floatValue];
        CGFloat height = [[size objectAtIndex:3] floatValue];
        _widget.frame = CGRectMake(left, top, width, height);
    }
}

- (void)Barcode:(PGMethod*)command {
    NSArray *args = command.arguments;
    NSString *cbID = [args objectAtIndex:0];
    NSArray *size = [args objectAtIndex:1];
        CGFloat left = [[size objectAtIndex:0] floatValue];
        CGFloat top = [[size objectAtIndex:1] floatValue];
        CGFloat width = [[size objectAtIndex:2] floatValue];
        CGFloat height = [[size objectAtIndex:3] floatValue];
    NSArray *filters = [args objectAtIndex:2];
    self.callBackID = cbID;
    
    NSSet *readers = [self supportReaders:filters];
    if ( nil == readers ) {
        PDRPluginResult *result = [PDRPluginResult resultWithStatus:PDRCommandStatusError
                                               messageToErrorObject:PGBarcodeErrorNotSupport];
        [self toCallback:cbID withReslut:[result toJSONString]];
        return;
    }
    
    if ( nil == _widget ) {  
        _widget = [[ZXingWidget alloc] initWithDelegate:self withFrame:CGRectMake(left, top, width, height) OneDMode:NO];
        _widget.clipsToBounds = YES;
        NSString *soundPath = [[NSBundle mainBundle] pathForResource:@"beep-beep" ofType:@"caf" inDirectory:@"PandoraApi.bundle"];
        if ( [soundPath length]) {
            _widget.soundToPlay = [NSURL fileURLWithPath:soundPath];
        }
        [_widget setTorch:NO];
        _widget.readers = readers;
        [self.JSFrameContext.webView.scrollView addSubview:_widget];
    }
    PDR_LOG_INFO(@"Barcode create-!!");
}

- (void)start:(PGMethod*)command {
    if (_widget && !self.scaning ) {
        NSDictionary *dict = [command.arguments objectAtIndex:0];
        if ( [dict isKindOfClass:[NSDictionary class]] ) {
            NSNumber *conserveV = [dict objectForKey:@"conserve"];
            if ( [conserveV isKindOfClass:[NSNumber class]] ) {
                self.decodeImgWToFile = YES;
            }
            if ( self.decodeImgWToFile ) {
                NSString *nameValue = [dict objectForKey:g_pdr_string_filename];
                if ( ![nameValue isKindOfClass:[NSString class]] ) {
                    nameValue = nil;
                } 
                self.decodeImgPath = [PTPathUtil absolutePath:nameValue prefix:@"barcode_" suffix:g_pdr_string_jpg context:nil];
            }
        }
        self.scaning = TRUE;
        [_widget resumeScan];
    }
}

- (void)cancel:(PGMethod*)command {
    if (_widget && self.scaning ) {
        self.decodeImgWToFile = NO;
        self.scaning = FALSE;
        [_widget pauseScan];
    }
}

- (void)setFlash:(PGMethod*)command {
    NSNumber *open = [command.arguments objectAtIndex:0];
    if ( _widget && [open isKindOfClass:[NSNumber class]]){
        [_widget setTorch:[open boolValue]];
    }
}

- (void)scan:(PGMethod*)command {
    
    NSString *cbID = [command.arguments objectAtIndex:0];
    NSString *argImgPath = [command.arguments objectAtIndex:1];
    NSArray *filters = [command.arguments objectAtIndex:2];
    PDRPluginResult *result = nil;
    if ( ![argImgPath isKindOfClass:NSString.class] ) {
        result = [PDRPluginResult resultWithStatus:PDRCommandStatusError messageToErrorObject:PGBarcodeErrorBadParam];
        [self toCallback:cbID withReslut:[result toJSONString]];
        return;
    }
    
    NSString *barcodeImgPath = [PTPathUtil absolutePath:argImgPath withContext:self.appContext];
    UIImage *barcodeImg = [UIImage imageWithContentsOfFile:barcodeImgPath];
    if ( !barcodeImg ) {
        result = [PDRPluginResult resultWithStatus:PDRCommandStatusError messageToErrorObject:PGBarcodeErrorImgNotFound];
        [self toCallback:cbID withReslut:[result toJSONString]];
        return;
    }
    
    NSSet *readers = [self supportReaders:filters];
    if ( nil == readers ) {
        PDRPluginResult *result = [PDRPluginResult resultWithStatus:PDRCommandStatusError
                                               messageToErrorObject:PGBarcodeErrorNotSupport];
        [self toCallback:cbID withReslut:[result toJSONString]];
        return;
    }

    DHDecode* d = [[DHDecode alloc] init];
    d.readers = readers;
    d.delegate = self;
    NSString *relPath = [PTPathUtil relativePath:barcodeImgPath];
    d.decImgPath = relPath ? relPath : barcodeImgPath;
    d.jsCBId = cbID;
    [d decodeImage:barcodeImg];
 //   [qrcodeReader release];
}

#pragma mark -
#pragma mark  image decoder delegate
- (void)decoder:(Decoder *)decoder
 didDecodeImage:(UIImage *)image
    usingSubset:(UIImage *)subset
     withResult:(TwoDDecoderResult *)twoDResult {
    DHDecode *dhDec = (DHDecode*)decoder;
    PDRPluginResult *result = [PDRPluginResult resultWithStatus:PDRCommandStatusOK
                                            messageAsDictionary:[self decodeResutWithText:twoDResult.text format:twoDResult.barcodeFormat file:dhDec.decImgPath]];
    [self toCallback:dhDec.jsCBId withReslut:[result toJSONString]];
    [decoder release];
}


- (void)decoder:(Decoder *)decoder
failedToDecodeImage:(UIImage *)image
    usingSubset:(UIImage *)subset reason:(NSString *)reason {
    DHDecode *dhDec = (DHDecode*)decoder;
    PDRPluginResult *result = [PDRPluginResult resultWithStatus:PDRCommandStatusError messageToErrorObject:PGBarcodeErrorDecodeError];
    [self toCallback:dhDec.jsCBId withReslut:[result toJSONString]];
    [decoder release];
}
#pragma mark -
#pragma mark  widget decoder delegate
- (void)zxingController:(ZXingWidget*)controller
        withDecodeImage:(UIImage*)dImg
          didScanResult:(NSString *)result
          didScanFormat:(BarcodeFormat)format {
    PDR_LOG_INFO(@"zxingController");
    [_widget pauseScan];
    NSString *relativeDecodeFilePath = nil;
    if ( self.decodeImgWToFile ) {
        NSString *decodeImgFilePath = self.decodeImgPath;// [PTPathUtil uniqueNameInAppDocHasPrefix:@"barcode" suffix:@"png"];
        if ( decodeImgFilePath ) {
            relativeDecodeFilePath = [PTPathUtil relativePath:decodeImgFilePath];
            UIImage *scrn = dImg;
            
            if ( UIDeviceOrientationPortrait == [UIDevice currentDevice].orientation ) {
                scrn = [[[UIImage alloc] initWithCGImage:dImg.CGImage
                                                   scale:1.0
                                             orientation:UIImageOrientationRight] autorelease];
            } else if ( UIDeviceOrientationLandscapeLeft == [UIDevice currentDevice].orientation) {
               /* if ( UIImageOrientationLeft == dImg.imageOrientation ) {
                    scrn = [[[UIImage alloc] initWithCGImage:dImg.CGImage
                                                       scale:1.0
                                                 orientation:UIImageOrientationRight] autorelease];
                }*/
            } else if ( UIDeviceOrientationLandscapeRight == [UIDevice currentDevice].orientation){
                //if ( UIImageOrientationLeft == dImg.imageOrientation ) {
                    scrn = [[[UIImage alloc] initWithCGImage:dImg.CGImage
                                                       scale:1.0
                                                 orientation:UIImageOrientationDown] autorelease];
               // }
            } else if (UIDeviceOrientationPortraitUpsideDown == [UIDevice currentDevice].orientation) {
                if ( UIImageOrientationUp == dImg.imageOrientation ) {
                    scrn = [[[UIImage alloc] initWithCGImage:dImg.CGImage
                                                       scale:1.0
                                                 orientation:UIImageOrientationLeft] autorelease];
                }
            }
            /*
            if ( UIImageOrientationLeft == dImg.imageOrientation ) {
                scrn = [[[UIImage alloc] initWithCGImage:dImg.CGImage
                                                   scale:1.0
                                             orientation:UIImageOrientationRight] autorelease];
            } else {
                
            }*/
            [UIImageJPEGRepresentation(/*[dImg imageRotatedByDegrees:90 supportRetina:NO scale:2.0]*/scrn, 0.5f)
             writeToFile:decodeImgFilePath atomically:NO];
        }
    }
    self.scaning = FALSE;
    PDRPluginResult *jsRet = [PDRPluginResult resultWithStatus:PDRCommandStatusOK
                                           messageAsDictionary:[self decodeResutWithText:result format:format file:relativeDecodeFilePath]];
    [jsRet setKeepCallback:YES];
    [self toCallback:self.callBackID withReslut:[jsRet toJSONString]];
    PDR_LOG_INFO(@"zxingController exec end");
}

#pragma mark -
#pragma mark tools
- (NSDictionary*)decodeResult:(TwoDDecoderResult*)twoDResult {
    return [self decodeResutWithText:twoDResult.text format:twoDResult.barcodeFormat file:nil];
}

- (NSDictionary*)decodeResutWithText:(NSString*)text format:(BarcodeFormat)barcodeFormat file:(NSString*)filePath {
    return [NSDictionary dictionaryWithObjectsAndKeys:text, g_pdr_string_message,
            [NSNumber numberWithInt:[self barcodeFormat2PG:barcodeFormat]], g_pdr_string_type,
            filePath?filePath:[NSNull null] , g_pdr_string_file,
            nil];
}

- (PGBarcodeFormat)barcodeFormat2PG: (BarcodeFormat)format {
    switch (format) {
        case BarcodeFormat_QR_CODE:
            return PGBarcodeFormatQR;
        case BarcodeFormat_EAN_8:
            return PGBarcodeFormatENA8;
        case BarcodeFormat_EAN_13:
            return PGBarcodeFormatENA13;
        case BarcodeFormat_AZTEC:
            return PGBarcodeFormatAZTEC;
        case BarcodeFormat_DATA_MATRIX:
            return PGBarcodeFormatDATAMATRIX;
        case BarcodeFormat_UPC_A:
            return PGBarcodeFormatUPCA;
        case BarcodeFormat_UPC_E:
            return PGBarcodeFormatUPCE;
        default:
            break;
    }
    return PGBarcodeFormatOther;
}

- (NSSet*)supportReaders:(NSArray*)filters {
    NSMutableArray *applyFilters = [NSMutableArray array];
    if ( [filters isKindOfClass:[NSArray class]] ) {
        for ( NSNumber *item in filters ) {
            if ( [item isKindOfClass:[NSNumber class]] ) {
                NSInteger type = [item intValue];
                if ( type >= 0 && type < PGBarcodeFormatOther ) {
                    [applyFilters addObject:item];
                }
            }
        }
    }
    
    if ( 0 == [applyFilters count] ) {
        [applyFilters addObject:[NSNumber numberWithInt:PGBarcodeFormatQR]];
        [applyFilters addObject:[NSNumber numberWithInt:PGBarcodeFormatENA13]];
        [applyFilters addObject:[NSNumber numberWithInt:PGBarcodeFormatENA8]];
    }
    
    NSUInteger appendBit = 0;
    NSMutableSet *readers = [[NSMutableSet alloc ] init];
    for ( NSNumber *filter in applyFilters ) {
        NSUInteger type = [filter intValue];
        switch (type) {
            case PGBarcodeFormatQR:
            {
                NSUInteger mask = (1 << PGBarcodeFormatQR);
                if ( 0 == (appendBit & mask) ) {
                    appendBit |= mask;
                    QRCodeReader* qrcodeReader = [[QRCodeReader alloc] init];
                    [readers addObject:qrcodeReader];
                    [qrcodeReader release];
                }
            }
                break;
            case PGBarcodeFormatENA13:
            case PGBarcodeFormatENA8:
            {
                NSUInteger mask = (1 << PGBarcodeFormatENA13);
                if ( 0 == (appendBit & mask) ) {
                    appendBit |= mask;
                    MultiFormatOneDReader *OneReaders=[[MultiFormatOneDReader alloc]init];
                    [readers addObject:OneReaders];
                    [OneReaders release];
                }
            }
                break;
            case PGBarcodeFormatAZTEC:
            {
                NSUInteger mask = (1 << PGBarcodeFormatAZTEC);
                if ( 0 == (appendBit & mask) ) {
                    appendBit |= mask;
                    AztecReader *aztecReader = [[AztecReader alloc] init];
                    [readers addObject:aztecReader];
                    [aztecReader release];
                    appendBit |= mask;
                }
            }
                break;
            case PGBarcodeFormatDATAMATRIX:
            {
                NSUInteger mask = (1 << PGBarcodeFormatDATAMATRIX);
                if ( 0 == (appendBit & mask) ) {
                    appendBit |= mask;
                    DataMatrixReader *dataMatrixReader = [[DataMatrixReader alloc] init];
                    [readers addObject:dataMatrixReader];
                    [dataMatrixReader release];
                }
            }
                break;
            case PGBarcodeFormatUPCA:
            case PGBarcodeFormatUPCE:
            {
                NSUInteger mask = (1 << PGBarcodeFormatUPCA);
                if ( 0 == (appendBit & mask) ) {
                    appendBit |= mask;
                    MultiFormatUPCEANReader *upceanReader = [[MultiFormatUPCEANReader alloc] init];
                    [readers addObject:upceanReader];
                    [upceanReader release];
                }
            }
                break;
            case PGBarcodeFormatCODABAR:
                
                break;
            case PGBarcodeFormatCODE39:
                
                break;
            case PGBarcodeFormatCODE93:
                
                break;
            case PGBarcodeFormatCODE128:
                
                break;
            case PGBarcodeFormatITF:
                
                break;
            case PGBarcodeFormatMAXICODE:
                
                break;
            case PGBarcodeFormatPDF417:
                
                break;
            case PGBarcodeFormatRSSEXPANDED:
                
                break;
            default:
                break;
        }
    }
    [readers autorelease];
    if ( 0 == [readers count] ) {
        return nil;
    }
    return readers;
}
- (void)dealloc {
    [_widget removeFromSuperview];
    [_widget release];
    self.callBackID = nil;
    self.decodeImgPath = nil;
    [super dealloc];
}

@end
