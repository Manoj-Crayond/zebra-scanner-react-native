package com.zebrascanner

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.zebra.barcode.sdk.sms.ConfigurationUpdateEvent
import com.zebra.scannercontrol.DCSSDKDefs
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_COMMAND_OPCODE
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.FirmwareUpdateEvent
import com.zebra.scannercontrol.IDcsSdkApiDelegate
import com.zebra.scannercontrol.SDKHandler

class Scanner(val scannerId: Int, val name: String)

class ZebraScannerManager(private val reactContext: ReactApplicationContext) : IDcsSdkApiDelegate {
    private lateinit var sdkHandler: SDKHandler
    val activeScanners: MutableList<Scanner> = ArrayList()

    init {
        UiThreadUtil.runOnUiThread {
            sdkHandler = SDKHandler(reactContext)
            sdkHandler.dcssdkSetDelegate(this)
            this.setupApi()
        }
    }

    // Public Functions
    fun setEnabled(isEnabled: Boolean) {
        for (item in activeScanners) {
            enableScanner(item.scannerId, isEnabled)
        }
    }

    fun getActiveScannerList(): List<Scanner> {
        return activeScanners
    }

    // Private Functions
    private fun setupApi() {
        var notifyMask = DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value
        try {
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
            sdkHandler.dcssdkEnableAvailableScannersDetection(true)
            sdkHandler.dcssdkSubsribeForEvents(notifyMask)

            // Get list of active scanners
            val scanners = sdkHandler.dcssdkGetActiveScannersList()
            Log.d(TAG, "Active Scanners: $scanners")
            for (scanner in scanners) {
                addScanner(scanner.scannerID, scanner.scannerName)
                Log.d(TAG, "Scanner detected in list: ID = ${scanner.scannerID}, Name = ${scanner.scannerName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in setupApi", e)
        }
    }

    private fun sendEvent(
        eventName: String,
        params: WritableMap
    ) {
        try {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in sendEvent", e)
        }
    }

    private fun sendEvent(eventName: String, scannerId: Int) {
        val params: WritableMap = WritableNativeMap()
        params.putInt("id", scannerId)

        sendEvent(eventName, params)
    }

    private fun enableScanner(scannerId: Int, isEnabled: Boolean) {
        val outXML = StringBuilder()
        var opCode = DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_DISABLE
        if (isEnabled) {
            opCode = DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_ENABLE
        }

        val inXml = "<inArgs><scannerID>$scannerId</scannerID></inArgs>"
        sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXml, outXML, scannerId)
    }

    private fun addScanner(scannerId: Int, name: String) {
        var scanner: Scanner? = null
        for (item in activeScanners) {
            if (item.scannerId == scannerId) {
                scanner = item
                break
            }
        }

        if (scanner == null) {
            activeScanners.add(Scanner(scannerId, name))
        }
    }

    private fun removeScanner(scannerId: Int) {
        var scanner: Scanner? = null
        for (item in activeScanners) {
            if (item.scannerId == scannerId) {
                scanner = item
                break
            }
        }
        if (scanner != null) {
            activeScanners.remove(scanner)
        }
    }

    // Scanner SDK Delegate Methods
    override fun dcssdkEventScannerAppeared(dcsScannerInfo: DCSScannerInfo) {
        Log.d(TAG, "Scanner appeared: ID = ${dcsScannerInfo.scannerID}, Name = ${dcsScannerInfo.scannerName}")
        addScanner(dcsScannerInfo.scannerID, dcsScannerInfo.scannerName)
        sdkHandler.dcssdkEstablishCommunicationSession(dcsScannerInfo.scannerID)
        sendEvent("scanner-appeared", dcsScannerInfo.scannerID)
        Log.d(TAG, "Attempting to establish communication session with scanner ID: ${dcsScannerInfo.scannerID}")
    }

    override fun dcssdkEventScannerDisappeared(scannerId: Int) {
        removeScanner(scannerId)
        sendEvent("scanner-disappeared", scannerId)
    }

    override fun dcssdkEventCommunicationSessionEstablished(dcsScannerInfo: DCSScannerInfo) {
        Log.d(TAG, "Scanner connected: ID = ${dcsScannerInfo.scannerID}, Name = ${dcsScannerInfo.scannerName}")
        sendEvent("scanner-connected", dcsScannerInfo.scannerID)
    }

    override fun dcssdkEventCommunicationSessionTerminated(scannerId: Int) {
        Log.d(TAG, "Scanner disconnected: ID = $scannerId")
        sendEvent("scanner-disconnected", scannerId)
    }

    override fun dcssdkEventBarcode(barcodeData: ByteArray, type: Int, scannerId: Int) {
        val params: WritableMap = WritableNativeMap()
        params.putInt("id", scannerId)
        params.putString("barcode", String(barcodeData))
        params.putInt("type", type)

        sendEvent("scanner-barcode", params)
    }

    override fun dcssdkEventImage(data: ByteArray, scannerId: Int) {
    }

    override fun dcssdkEventVideo(data: ByteArray, scannerId: Int) {
    }

    override fun dcssdkEventBinaryData(data: ByteArray, scannerId: Int) {
    }

    override fun dcssdkEventFirmwareUpdate(firmwareUpdateEvent: FirmwareUpdateEvent) {
    }

    override fun dcssdkEventAuxScannerAppeared(
        dcsScannerInfo: DCSScannerInfo,
        dcsScannerInfo1: DCSScannerInfo
    ) {
    }

    override fun dcssdkEventConfigurationUpdate(p0: ConfigurationUpdateEvent?) {

    }

    companion object {
        private const val TAG = "ZebraScannerManager"
    }
}
