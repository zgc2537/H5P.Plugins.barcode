package io.dcloud.feature.barcode;

import io.dcloud.DHInterface.IEventCallback;
import io.dcloud.DHInterface.IWebview;
import io.dcloud.adapter.ui.AdaFrameItem;
import io.dcloud.adapter.util.DeviceInfo;
import io.dcloud.adapter.util.Logger;
import io.dcloud.constant.StringConst;
import io.dcloud.feature.barcode.camera.CameraManager;
import io.dcloud.feature.barcode.decoding.CaptureActivityHandler;
import io.dcloud.feature.barcode.decoding.IBarHandler;
import io.dcloud.feature.barcode.decoding.InactivityTimer;
import io.dcloud.feature.barcode.view.DetectorViewConfig;
import io.dcloud.feature.barcode.view.ViewfinderView;
import io.dcloud.util.JSONUtil;
import io.dcloud.util.JSUtil;
import io.dcloud.util.PdrUtil;

import java.io.IOException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsoluteLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

class BarcodeFrameItem extends AdaFrameItem implements Callback,IBarHandler{
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	SurfaceView surfaceView;
	String mCallbackId = null;
	private Context mAct;
	private IWebview mWebViewImpl;
	/**是否处于待扫描状态*/
	private boolean mRunning = false;
	public String errorMsg = null;
	boolean mConserve = false;
	String mFilename = null;
	BarcodeProxy mProxy;
	static BarcodeFrameItem sBarcodeFrameItem = null;
	protected BarcodeFrameItem(BarcodeProxy pProxy,IWebview pWebViewImpl,LayoutParams lp,JSONArray filters) {
		super(pWebViewImpl.getContext());
		sBarcodeFrameItem = this;
		mProxy = pProxy;
		mAct = pWebViewImpl.getContext();
		mWebViewImpl = pWebViewImpl;
		AbsoluteLayout mainView = new AbsoluteLayout(mAct);
		setMainView(mainView);
		surfaceView = new SurfaceView(mAct){
			/**解决第一次出现闪一下问题*/
			boolean show = false;
			@Override
			protected void dispatchDraw(Canvas canvas) {
				if(show){
					super.dispatchDraw(canvas);
				}else{
					show = true; 
					postDelayed(new Runnable(){
						@Override
						public void run() {
							invalidate();
						}}, 1);
				}
			}
		};
		viewfinderView = new ViewfinderView(mAct,this);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(getActivity());
		CameraManager.init(getActivity().getApplication());
		Point screenResolution = CameraManager.getCR();
		float rw = (float)lp.width / screenResolution.y  ;
		float rh = (float)lp.height / screenResolution.x  ;
		int bestWidth = Math.max(screenResolution.y, lp.width);
		int bestHeight = Math.max(screenResolution.x, lp.height);
		if(rw > rh){
			bestWidth = (int)( screenResolution.y * rw);
			bestHeight = (int)(screenResolution.x * rw);
		}else if(rw < rh){
			bestWidth = (int)( screenResolution.y * rh);
			bestHeight = (int)( screenResolution.x * rh);
		}
		CameraManager.get().reviseScreenResolution(new Point(bestWidth, bestHeight));//校正取值
		mainView.addView(surfaceView,new LayoutParams(bestWidth, bestHeight));
		mainView.addView(viewfinderView);
		//CameraManager
		initDecodeFormats(filters);
		onResume(false);//启动预览，绘制探测区域
		saveOrientationState();//记录进来时候屏幕重力感应设置，退出时候进行还原
		isVerticalScreen = DeviceInfo.isVerticalScreen();
		if(isVerticalScreen){//竖屏进来
			DeviceInfo.setRequestedOrientation("portrait");
		}else{//横屏进来
			DeviceInfo.setRequestedOrientation("landscape");
		}
		listenHideAndShow(pWebViewImpl);
	}
	
	private void listenHideAndShow(IWebview pWebViewImpl){
		pWebViewImpl.obtainFrameView().addFrameViewListener(new IEventCallback() {
			@Override
			public Object onCallBack(String pEventType, Object pArgs) {
				if(PdrUtil.isEquals(pEventType, StringConst.EVENTS_WEBVIEW_HIDE)){
					onPause();
				}else if(PdrUtil.isEquals(pEventType, StringConst.EVENTS_SHOW_ANIMATION_END)){
					onResume(true);
				}
				return null;
			}
		});
	}
	@Override
	public void autoFocus() {
		handler.autoFocus();
	}
	@Override
	public void dispose() {
		super.dispose();
		Logger.d("Barcode","dispose");
		onPause();
		DetectorViewConfig.clearData();
		mProxy.mBarcodeView = null;
		surfaceView = null;
		resumeOrientationState();
	}
	/**是否是竖屏*/
	boolean isVerticalScreen = true;
	int mOrientationState;
	/** 保存当前设置的重力感应状态*/
	private void saveOrientationState(){
		mOrientationState = DeviceInfo.getRequestedOrientation();
	}
	/**恢复进入扫描页面前的重力感应状态*/
	private void resumeOrientationState(){
		DeviceInfo.setRequestedOrientation(mOrientationState);
	}
	protected void onResume(boolean isSysEvent){
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
//		decodeFormats = null;
//		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) mAct.getSystemService(Activity.AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
		if(isSysEvent){//系统事件过来的通知
			if(mRunning){//系统时间过来的时候处于扫描状态
				mRunning = false;//认为设置处于非扫描状态，因为onpause事件可能引起扫描状态改变
				start();
			}
		}
	}
	
	protected void onPause(){
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
		boolean t = mRunning;//保存取消前的扫描状态
		cancel();
		mRunning = t;//恢复扫描状态
	}
	
	protected void start() {
		if(!mRunning){
			getViewfinderView().startUpdateScreenTimer();
			if (handler != null) {
				handler.restartPreviewAndDecode();
			}
			mRunning = true;
		}
	}

	public void setFlash(boolean enable){
		CameraManager.get().setFlashlight(enable);
	}
	protected void cancel() {
		if(mRunning){
			if (handler != null) {
				handler.stopDecode();
			}
			getViewfinderView().stopUpdateScreenTimer();
			mRunning = false;
		}
	}

	protected void onDestroy() {
		inactivityTimer.shutdown();
		hasSurface = false;
		decodeFormats = null;
		characterSet = null;
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			errorMsg = ioe.getMessage();
			return;
		} catch (RuntimeException e) {
			errorMsg = e.getMessage();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
			if (mRunning && handler != null) {//可能start的调用早于此处运行
				handler.restartPreviewAndDecode();
			}
		}else{
			handler.resume(mRunning);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}
	@Override
	public boolean isRunning() {
		return mRunning;
	}
	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}
	public void handleDecode(Result obj, Bitmap barcode) {
		inactivityTimer.onActivity();
		 playBeepSoundAndVibrate();
		 boolean saveSuc = false;
		 if(mConserve){
			 saveSuc = PdrUtil.saveBitmapToFile(barcode, mFilename);
//			 PdrUtil.showNativeAlert(mWebViewImpl.getContext(), "获取的扫描信息", barcode);
		 }
		 int num = convertTypestrToNum(obj.getBarcodeFormat().toString());
		 String json = null;
		 if(saveSuc){
			 String message = "{type:%d,message:%s,file:'%s'}";
			 String doc  = mWebViewImpl.obtainFrameView().obtainApp().obtainAppDocPath();
			 Logger.d("doc:" + doc);
			 if(mFilename.startsWith(doc)){
				 mFilename = "_doc" + mFilename.substring(doc.length()-1);
			 }
			 String relPath =  mWebViewImpl.obtainFrameView().obtainApp().convert2RelPath(mFilename);
			 Logger.d("Filename:" + mFilename + ";relPath:" + relPath);
			 json = String.format(message, num,JSONUtil.toJSONableString(obj.getText()),relPath); 
		 }else{
			 String message = "{type:%d,message:%s}";
			 json = String.format(message, num,JSONUtil.toJSONableString(obj.getText())); 
		 }
		 JSUtil.execCallback(mWebViewImpl, mCallbackId, json, JSUtil.OK, true, true);
		 cancel();//start一次只能有一次结果，所以成功之后需要停止
	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);
			
			try {
				AssetFileDescriptor file = mAct.getResources().getAssets().openFd(StringConst.RES_BEEP);
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) mAct.getSystemService(mAct.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};
	
	private int convertTypestrToNum(String type){
		if(type.indexOf("QR_CODE") >= 0){
			return QR;
		}else if(type.indexOf("EAN_13") >= 0){
			return EAN13;
		}else if(type.indexOf("EAN_8") >= 0){
			return EAN8;
		}
		return UNKOWN;
	}
	
	
	
	private void initDecodeFormats(JSONArray filters){
		decodeFormats = new Vector<BarcodeFormat>();
		if(filters == null || filters.length() == 0){//默认支持
			decodeFormats.add(BarcodeFormat.EAN_13);
			decodeFormats.add(BarcodeFormat.EAN_8);
			decodeFormats.add(BarcodeFormat.QR_CODE);
		}else{
			int size = filters.length();
			for(int i = 0; i < size; i++){
				int filter = -1;
				try {
					filter = filters.getInt(i);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if(filter != -1){
					decodeFormats.add(convertNumToBarcodeFormat(filter));
				}
			}
		}
	}

	private BarcodeFormat convertNumToBarcodeFormat(int num){
		BarcodeFormat _ret = null;
		switch (num) {
		case QR :{
			_ret = BarcodeFormat.QR_CODE;
			break;
		}
	    case EAN13 :{
			_ret = BarcodeFormat.EAN_13;
			break;
		}
	    case EAN8 :{
			_ret = BarcodeFormat.EAN_8;
			break;
		}
	    case AZTEC :{
			_ret = BarcodeFormat.AZTEC;
			break;
		}
	    case DATAMATRIX :{
			_ret = BarcodeFormat.DATA_MATRIX;
			break;
		}
	    case UPCA :{
			_ret = BarcodeFormat.UPC_A;
			break;
		}
	    case UPCE :{
			_ret = BarcodeFormat.UPC_E;
			break;
		}
	    case CODABAR :{
			_ret = BarcodeFormat.CODABAR;
			break;
		}
	    case CODE39 :{
			_ret = BarcodeFormat.CODE_39;
			break;
		}
	    case CODE93 :{
			_ret = BarcodeFormat.CODE_93;
			break;
		}
	    case CODE128 :{
			_ret = BarcodeFormat.CODE_128;
			break;
		}
	    case ITF :{
			_ret = BarcodeFormat.ITF;
			break;
		}
	    case MAXICODE :{
			_ret = BarcodeFormat.MAXICODE;
			break;
		}
	    case PDF417 :{
			_ret = BarcodeFormat.PDF_417;
			break;
		}
	    case RSS14 :{
			_ret = BarcodeFormat.RSS_14;
			break;
		}
	    case RSSEXPANDED :{
			_ret = BarcodeFormat.RSS_EXPANDED;
			break;
		}
		}
		return _ret;
	}
	
	static final int UNKOWN      = -1000;
	static final int QR = 0;
    static final int EAN13 = 1;
    static final int EAN8 = 2;
    static final int AZTEC = 3;
    static final int DATAMATRIX = 4;
    static final int UPCA = 5;
    static final int UPCE = 6;
    static final int CODABAR = 7;
    static final int CODE39 = 8;
    static final int CODE93 = 9;
    static final int CODE128 = 10;
    static final int ITF = 11;
    static final int MAXICODE = 12;
    static final int PDF417 = 13;
    static final int RSS14 = 14;
    static final int RSSEXPANDED = 15;

}
