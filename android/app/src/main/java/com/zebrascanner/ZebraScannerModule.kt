package com.zebrascanner

import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap

class ZebraScannerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(
        reactContext
    ) {
    private val scannerManager: ZebraScannerManager = ZebraScannerManager(reactContext)

    override fun getName(): String {
        return "ZebraScanner"
    }

    // Ensure that all UI-related methods are run on the main thread
    @ReactMethod
    fun setEnabled(isEnabled: Boolean) {
        UiThreadUtil.runOnUiThread {
            scannerManager.setEnabled(isEnabled)
        }
    }

    @ReactMethod
    fun getActiveScanners(callback: Callback) {
        UiThreadUtil.runOnUiThread {
            val scanners = scannerManager.activeScanners
            val writableScanners: WritableArray = WritableNativeArray()

            for (item in scanners) {
                val scannerMap: WritableMap = WritableNativeMap()
                scannerMap.putInt("id", item.scannerId)
                scannerMap.putString("name", item.name)
                writableScanners.pushMap(scannerMap)
            }

            callback.invoke(writableScanners)
        }
    }

}
