;(function(plus){
    var _BARCODE = 'barcode',
		B = window.plus.bridge;

    function Barcode(id, filters ){
		var me = this;
        me.onmarked = null;
        me.onerror = null;
        
        var callbackId = B.callbackId( function(args){
				if( typeof me.onmarked === 'function' ){
					me.onmarked(args.type, args.message,args.file);
				}
			}, function(code){
				if ( typeof me.onerror === 'function' ) {
					me.onerror(code);
				}
			}),
			div = document.getElementById(id);
        div.addEventListener("resize", function(){
            var args = [div.offsetLeft, div.offsetTop,div.offsetWidth,div.offsetHeight];
            B.exec( _BARCODE, "resize", [args]);
        }, false);
        var args = [div.offsetLeft, div.offsetTop, div.offsetWidth, div.offsetHeight];
        B.exec( _BARCODE, "Barcode", [callbackId,args, filters ] );
    };
	var proto = Barcode.prototype;
    proto.start = function(options) {
        B.exec( _BARCODE, "start", [options] );
    };

    proto.setFlash = function(open) {
        B.exec( _BARCODE, "setFlash", [open] );
    };

    proto.cancel = function() {
        B.exec( _BARCODE, "cancel", [] );
    };

    var barcode = {
		Barcode : Barcode,
		scan : function (path, successCallback, errorCallback, filters ) {
				var success = typeof successCallback !== 'function' ? null : function(args) {
						successCallback(args.type, args.message, args.file);
					},
					fail = typeof errorCallback !== 'function' ? null : function(code) {
						errorCallback(code);
					},
					callbackID = B.callbackId(success, fail);
				B.exec(_BARCODE, "scan", [callbackID, path, filters ]);
		}
    };
    barcode.QR = 0;
    barcode.EAN13 = 1;
    barcode.EAN8 = 2;
    barcode.AZTEC = 3;
    barcode.DATAMATRIX = 4;
    barcode.UPCA = 5;
    barcode.UPCE = 6;
    barcode.CODABAR = 7;
    barcode.CODE39 = 8;
    barcode.CODE93 = 9;
    barcode.CODE128 = 10;
    barcode.ITF = 11;
    barcode.MAXICODE = 12;
    barcode.PDF417 = 13;
    barcode.RSS14 = 14;
    barcode.RSSEXPANDED = 15;

    plus.barcode = barcode;
 })(window.plus);