package io.dcloud.feature.barcode.view;

import android.graphics.Rect;

public class DetectorViewConfig {
	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 640;
	private static final int MAX_FRAME_HEIGHT = 360;
	
	public static int maskColor = 0x60000000;
	public static int laserColor_ = 0x00ff0000;
	public static int laserColor = 0xffff0000;
	public static int cornerColor = laserColor;
	public static int resultPointColor = 0xc0ffff00;
	
	public static int CORNER_WIDTH = 8;
	public static int CORNER_HEIGHT = 40;
	
	public static int LASER_WIDTH = 8;
	/**数据采集区域*/
	public Rect gatherRect = new Rect();
	private Rect detectorRect = null;

	private static DetectorViewConfig instance;
	private DetectorViewConfig(){
	}
	public static DetectorViewConfig getInstance(){
		if(instance == null){
			instance = new DetectorViewConfig();
		}
		return instance;
	}
	
	public static void clearData(){
		instance = null;
	}
	
	public Rect getDetectorRect(){
		if(detectorRect == null){
			int gatherAreaW = gatherRect.width() - CORNER_WIDTH;
			int gatherAreaH = gatherRect.height() - CORNER_WIDTH;
			int width = gatherAreaW * 6 / 10;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = width;
			CORNER_HEIGHT = width * 10 / 100;
			int leftOffset = (gatherAreaW - width) / 2;
			int topOffset = (gatherAreaH - height) / 2;
			detectorRect = new Rect(leftOffset, topOffset, leftOffset + width,topOffset + height);
		}
		return detectorRect;
	}
}
