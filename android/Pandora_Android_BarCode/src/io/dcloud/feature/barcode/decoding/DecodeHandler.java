/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dcloud.feature.barcode.decoding;

import io.dcloud.feature.barcode.camera.CameraManager;
import io.dcloud.feature.barcode.camera.PlanarYUVLuminanceSource;

import java.util.Hashtable;
import java.util.Vector;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final IBarHandler activity;
  private final MultiFormatReader multiFormatReader;

  DecodeHandler(IBarHandler activity, Hashtable<DecodeHintType, Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case CaptureActivityHandler.CODE_DECODE:
        //Log.d(TAG, "Got decode message");
        decode((byte[]) message.obj, message.arg1, message.arg2);
        break;
      case CaptureActivityHandler.CODE_QUIT:
        Looper.myLooper().quit();
        break;
    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    Result rawResult = null;
    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);

    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    try {
      rawResult = multiFormatReader.decodeWithState(bitmap);
    } catch (ReaderException re) {
      // continue
    } finally {
      multiFormatReader.reset();
    }

    if (rawResult != null) {
      Message message = Message.obtain(activity.getHandler(), CaptureActivityHandler.CODE_DECODE_SUCCEEDED, rawResult);
      //返回图片信息---begin
      Bundle bundle = new Bundle();
      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap(true));//因为相机预览获取的数据为为横屏模式下数据，此时需要进行翻转
      message.setData(bundle);
//      //Log.d(TAG, "Sending decode succeeded message...");
      message.sendToTarget();
      //----------end
    } else {
      Message message = Message.obtain(activity.getHandler(), CaptureActivityHandler.CODE_DECODE_FAILED);
      message.sendToTarget();
    }
  }

	public static Result decode(Bitmap map) {
		MultiFormatReader multiFormatReader = new MultiFormatReader();

		// 解码的参数
		Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(2);
		// 可以解析的编码类型
		Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
		if (decodeFormats == null || decodeFormats.isEmpty()) {
			decodeFormats = new Vector<BarcodeFormat>();

			// 这里设置可扫描的类型，我这里选择了都支持
			decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
			decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
			decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
		}
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

		// 设置继续的字符编码格式为UTF8
		// hints.put(DecodeHintType.CHARACTER_SET, "UTF8");

		// 设置解析配置参数
		multiFormatReader.setHints(hints);
		// 开始对图像资源解码
		Result rawResult = null;
		try {
			rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(
					new HybridBinarizer(new BitmapLuminanceSource(map))));
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return rawResult;
	}
}
