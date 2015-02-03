package io.dcloud.feature.barcode;

import io.dcloud.DHInterface.IApp;
import io.dcloud.DHInterface.ISysEventListener;
import io.dcloud.DHInterface.IWebview;
import io.dcloud.adapter.ui.AdaFrameItem;
import io.dcloud.adapter.ui.AdaWebViewParent;
import io.dcloud.adapter.ui.AdaFrameItem.LayoutParamsUtil;
import io.dcloud.adapter.util.Logger;
import io.dcloud.adapter.util.ViewRect;
import io.dcloud.constant.DOMException;
import io.dcloud.feature.barcode.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode.view.DetectorViewConfig;
import io.dcloud.util.JSONUtil;
import io.dcloud.util.JSUtil;
import io.dcloud.util.PdrUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.view.ViewGroup.LayoutParams;

import com.google.zxing.Result;

public class BarcodeProxy implements ISysEventListener {
	
	BarcodeFrameItem mBarcodeView = null;
	boolean mIsRegisetedSysEvent = false;
	void execute(IWebview pWebViewImpl, String pActionName,String[] pJsArgs){
		if("start".equals(pActionName)){
			if(!PdrUtil.isEmpty(mBarcodeView.errorMsg)){
				String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BARCODE_ERROR,mBarcodeView.errorMsg);
				JSUtil.execCallback(pWebViewImpl, mBarcodeView.mCallbackId,msg , JSUtil.ERROR, true, true);
			}else{
				boolean _conserve = false;
				JSONObject args = JSONUtil.createJSONObject(pJsArgs[0]);
				if(args != null){
					_conserve = PdrUtil.parseBoolean(JSONUtil.getString(args, "conserve"), _conserve, false);
					if(_conserve){
						String _filename = PdrUtil.getDefaultPrivateDocPath(JSONUtil.getString(args, "filename"), "png");
						mBarcodeView.mFilename = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),_filename);
						Logger.d("Filename:" + mBarcodeView.mFilename);
					}
				}
				mBarcodeView.mConserve = _conserve;
				mBarcodeView.start();
			}
		}else if("cancel".equals(pActionName)){
			mBarcodeView.cancel();
		}else if("setFlash".equals(pActionName)){
			mBarcodeView.setFlash(Boolean.parseBoolean(pJsArgs[0]));
		}else if("Barcode".equals(pActionName)){
			if(!mIsRegisetedSysEvent){
				IApp app = pWebViewImpl.obtainFrameView().obtainApp();
				app.registerSysEventListener(this, SysEventType.onPause);
				app.registerSysEventListener(this, SysEventType.onResume);
				mIsRegisetedSysEvent = true;
			}
			//解析html控件位置大小
			JSONArray arr = JSONUtil.createJSONArray(pJsArgs[1]);
			Rect dvc = DetectorViewConfig.getInstance().gatherRect;
			dvc.left = PdrUtil.parseInt(JSONUtil.getString(arr, 0), 0);
			dvc.top = PdrUtil.parseInt(JSONUtil.getString(arr, 1), 0);
			dvc.right = dvc.left + PdrUtil.parseInt(JSONUtil.getString(arr, 2), 0);
			dvc.bottom = dvc.top + PdrUtil.parseInt(JSONUtil.getString(arr, 3), 0);
			
			float s = pWebViewImpl.getScale();
			dvc.left *= s;
			dvc.top *= s;
			dvc.right *= s; 
			dvc.bottom *= s;
			if(dvc.width() != 0 && dvc.height() != 0){
				//创建barcode系统控件
				JSONArray filters = null;
				if(!PdrUtil.isEmpty(pJsArgs[2])){
					filters = JSONUtil.createJSONArray(pJsArgs[2]);//获取支持扫描
				}
				LayoutParams lp = LayoutParamsUtil.createLayoutParams(dvc.left, dvc.top, dvc.width(), dvc.height());
				mBarcodeView = new BarcodeFrameItem(this,pWebViewImpl,lp,filters);
				AdaFrameItem frameView = (AdaFrameItem)pWebViewImpl.obtainFrameView();
				ViewRect frameViewRect = frameView.obtainFrameOptions();
				mBarcodeView.updateViewRect((AdaFrameItem)pWebViewImpl.obtainFrameView(), new int[]{dvc.left,dvc.top,dvc.width(),dvc.height()}, new int[]{frameViewRect.width,frameViewRect.height});
				mBarcodeView.mCallbackId = pJsArgs[0];
				pWebViewImpl.obtainFrameView().addFrameItem(mBarcodeView,lp);
//				pWebViewImpl.addFrameItem(mBarcodeView,lp);
			}else{
				Logger.e("Barcode","LayoutParams l=" + dvc.left + ";t=" + dvc.top + ";r=" + dvc.right + ";b=" + dvc.bottom);
				//创建失败
			}
		}else if("scan".equals(pActionName)){
			String callbackId = pJsArgs[0];
			IApp app = pWebViewImpl.obtainFrameView().obtainApp();
			String path = app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pJsArgs[1]);
			Bitmap map = BitmapFactory.decodeFile(path);
			Result result = CaptureActivityHandler.decode(map);
			if(result != null){
				String message = "{type:'%s',message:'%s',file:'%s'}";
				 message = String.format(message, result.getBarcodeFormat().toString(),result.getText(),path); 
				 JSUtil.execCallback(pWebViewImpl, callbackId, message, JSUtil.OK, true, false);
			}else{
				String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BARCODE_ERROR,DOMException.MSG_BARCODE);
				JSUtil.execCallback(pWebViewImpl, callbackId,msg , JSUtil.ERROR, true, false);
			}
		}
	}
	
	protected void onDestroy() {
		if(mBarcodeView != null){
			mBarcodeView.onDestroy();
			mBarcodeView = null;
		}
		mIsRegisetedSysEvent = false;
	}
	
	protected void onPause() {
		if(mBarcodeView != null)
			mBarcodeView.onPause();
	}
	protected void onResume() {
		if(mBarcodeView != null)
			mBarcodeView.onResume(true);
	}
	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
		if(pEventType == SysEventType.onResume){
			onResume();
		}else if(pEventType == SysEventType.onPause){
			onPause();
		}
		return false;
	}

}