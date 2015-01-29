/*
 *------------------------------------------------------------------
 *  pandora/PGShare.h
 *  Description:
 *      上传插件头文件定义
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

#import "PGPlugin.h"
#import "PGMethod.h"
#import "ZXingWidget.h"

typedef NS_ENUM(NSInteger, PGBarcodeFormat) {
    //二维QR码，1994年由日本Denso-Wave公司发明，
    //QR来自英文Quick Response的缩写，即快速反应的意思，源自发明者希望QR码可让其内容快速被解码
    PGBarcodeFormatQR = 0,
    //EAN码标准版，由国际物品编码协会在全球推广应用的商品条码，是由13位数字组成
    PGBarcodeFormatENA13 = 1,
    //EAN缩短版，由国际物品编码协会在全球推广应用的商品条码，是由8位数字组成
    PGBarcodeFormatENA8 = 2,
    PGBarcodeFormatAZTEC,
    PGBarcodeFormatDATAMATRIX,
    PGBarcodeFormatUPCA,
    PGBarcodeFormatUPCE,
    PGBarcodeFormatCODABAR,
    PGBarcodeFormatCODE39,
    PGBarcodeFormatCODE93,
    PGBarcodeFormatCODE128,
    PGBarcodeFormatITF,
    PGBarcodeFormatMAXICODE,
    PGBarcodeFormatPDF417,
    PGBarcodeFormatRSS14,
    PGBarcodeFormatRSSEXPANDED,
    PGBarcodeFormatOther
};

typedef NS_ENUM(NSInteger, PGBarcodeError) {
    PGBarcodeErrorNO = 0, //ok
    PGBarcodeErrorBadParam = 1,
    PGBarcodeErrorImgNotFound = 2,
    PGBarcodeErrorDecodeError = 3,
    PGBarcodeErrorNotSupport = 4
};

@interface  PGBarcode : PGPlugin <ZXingDelegate,DecoderDelegate>{
    ZXingWidget *_widget;
}

@property(nonatomic, copy)NSString *callBackID;
@property(nonatomic, assign)BOOL scaning;
@property(nonatomic, assign)BOOL decodeImgWToFile;
@property(nonatomic, copy)NSString *decodeImgPath;
- (void)Barcode:(PGMethod*)command;
- (void)start:(PGMethod*)command;
- (void)cancel:(PGMethod*)command;
- (void)setFlash:(PGMethod*)command;
- (void)scan:(PGMethod*)command;
@end