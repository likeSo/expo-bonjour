package expo.modules.bonjour

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition


class ExpoBonjourModule : Module() {
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredDevices = mutableMapOf<String, NsdServiceInfo>()
    private val resolvedDevices = mutableMapOf<String, Map<String, Any?>>()
    private var isScanning = false
    private var isPublishing = false
    private var registrationListener: RegistrationListener? = null

    private val context: Context
        get() = appContext.reactContext ?: throw Exception("React context is null")

    override fun definition() = ModuleDefinition {
        Name("ExpoBonjour")

        Events(
            "onStartScan",
            "onScanError",
            "onStopScan",
            "onDeviceFound",
            "onDeviceChange",
            "onDeviceRemoved",
            "onPublishingError"
        )

        OnCreate {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        }

        OnDestroy {
            stopScanInternal()
        }

        AsyncFunction("startScan") { serviceType: String, protocol: String, domain: String? ->
            if (isScanning) {
                stopScanInternal()
            }

            val type = "$serviceType.$protocol"

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    isScanning = true
                    sendEvent("onStartScan")
                    Log.d("Bonjour", "开始发现")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.d("Bonjour", "找到设备：$serviceInfo")
                    val key = getDeviceKey(serviceInfo)
                    discoveredDevices[key] = serviceInfo

                    // 解析服务以获取完整信息
                    resolveService(serviceInfo) { resolvedInfo ->
                        resolvedInfo?.let {
                            resolvedDevices[key] = it
                            sendEvent("onDeviceFound", it)
                        }
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val key = getDeviceKey(serviceInfo)
                    discoveredDevices.remove(key)
                    val removedDevice = resolvedDevices.remove(key)

                    if (removedDevice != null) {
                        sendEvent("onDeviceRemoved", removedDevice)
                    } else {
                        sendEvent("onDeviceRemoved", parseServiceInfo(serviceInfo))
                    }
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    isScanning = false
                    sendEvent("onStopScan")
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    isScanning = false
                    Log.d("Bonjour", "报错了：$errorCode")
                    sendEvent(
                        "onScanError", mapOf(
                            "errorCode" to errorCode,
                            "type" to "START_DISCOVERY_FAILED",
                            "message" to "Failed to start discovery: $errorCode"
                        )
                    )
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    sendEvent(
                        "onScanError", mapOf(
                            "errorCode" to errorCode,
                            "type" to "STOP_DISCOVERY_FAILED",
                            "message" to "Failed to stop discovery: $errorCode"
                        )
                    )
                }
            }

            try {
                nsdManager?.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            } catch (e: Exception) {
                isScanning = false
                sendEvent(
                    "onScanError", mapOf(
                        "errorCode" to -1,
                        "type" to "EXCEPTION",
                        "message" to (e.message ?: "Unknown error")
                    )
                )
                throw e
            }
        }

        AsyncFunction("stopScan") {
            stopScanInternal()
        }

        AsyncFunction("getAllDevices") {
            if (!isScanning && discoveredDevices.isEmpty()) {
                throw CodedException("ERR_NO_BROWSER", "Please call startScan() first.", null)
            }
            return@AsyncFunction resolvedDevices.values.toList()
        }

        AsyncFunction("getIPAddress") { device: Map<String, Any?>, promise: Promise ->
            try {
                if (!isScanning && discoveredDevices.isEmpty()) {
                    promise.reject(
                        "ERR_NO_BROWSER_OR_DEVICE",
                        "No browser or devices now. Please make sure call startScan() first and at least 1 device found.",
                        null
                    )
                    return@AsyncFunction
                }

                // 查找目标设备
                val fullName = device["fullName"] as? String
                val targetKey = discoveredDevices.keys.find { key ->
                    val info = discoveredDevices[key]
                    val infoFullName =
                        "${info?.serviceName}.${info?.serviceType}${info?.host?.hostName ?: ""}"
                    fullName == infoFullName || isSameDevice(resolvedDevices[key], device)
                }

                if (targetKey == null) {
                    promise.reject(
                        "ERR_NO_DEVICE_FOUND",
                        "No target device found, please make sure your device info is the latest.",
                        null
                    )
                    return@AsyncFunction
                }

                val serviceInfo = discoveredDevices[targetKey]
                if (serviceInfo == null) {
                    promise.reject("ERR_NO_DEVICE_FOUND", "Device not found", null)
                    return@AsyncFunction
                }

                // 如果已经解析过，直接返回
                val resolvedInfo = resolvedDevices[targetKey]
                if (resolvedInfo != null && resolvedInfo["host"] != null) {
                    promise.resolve(resolvedInfo)
                    return@AsyncFunction
                }

                // 重新解析服务以获取最新的 IP 地址
                resolveService(serviceInfo) { resolvedInfo ->
                    if (resolvedInfo != null) {
                        resolvedDevices[targetKey] = resolvedInfo
                        promise.resolve(resolvedInfo)
                    } else {
                        promise.reject("ERR_CANNOT_GET", "Cannot resolve service", null)
                    }
                }
            } catch (e: Exception) {
                promise.reject("ERR_CANNOT_GET", e.message ?: "Unknown error", e)
            }
        }


        AsyncFunction("startPublish") { options: PublishingOptions ->
            if (isPublishing) {
                stopPublishInternal()
            }
            val serviceInfo = NsdServiceInfo()
            serviceInfo.serviceName = options.name
            serviceInfo.serviceType = options.service
            serviceInfo.port = options.port

            if (!options.txtRecord.isNullOrEmpty()) {
                options.txtRecord?.forEach {
                    serviceInfo.setAttribute(it.key, it.value)
                }
            }
            registrationListener = object : RegistrationListener {
                override fun onServiceRegistered(registeredService: NsdServiceInfo) {
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    sendEvent("onPublishError", mapOf("errorCode" to errorCode))
                    isPublishing = false
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                }
            }
            isPublishing = true
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        }

        AsyncFunction("stopPublish") {
            stopPublishInternal()
        }
    }

    private fun resolveService(
        serviceInfo: NsdServiceInfo,
        callback: (Map<String, Any?>?) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                callback(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val deviceInfo = parseResolvedServiceInfo(serviceInfo)
                callback(deviceInfo)
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            callback(null)
        }
    }

    private fun parseServiceInfo(serviceInfo: NsdServiceInfo): Map<String, Any?> {
        return mapOf(
            "name" to serviceInfo.serviceName,
            "type" to serviceInfo.serviceType,
            "domain" to "local.",
            "fullName" to "${serviceInfo.serviceName}.${serviceInfo.serviceType}local.",
            "txt" to emptyMap<String, String>(),
            "address" to "",
            "port" to 0
        )
    }

    private fun parseResolvedServiceInfo(serviceInfo: NsdServiceInfo): Map<String, Any?> {
        val txtRecords = mutableMapOf<String, String>()
        try {
            serviceInfo.attributes.forEach { (key, value) ->
                txtRecords[key] = value?.let { String(it, Charsets.UTF_8) } ?: ""
            }
        } catch (e: Exception) {
            // TXT records parsing failed
        }

        val host = serviceInfo.host
        val hostString = host?.hostAddress ?: host?.hostName ?: ""
        val port = serviceInfo.port

        return mapOf(
            "name" to serviceInfo.serviceName,
            "type" to serviceInfo.serviceType,
            "domain" to "local.",
            "fullName" to "${serviceInfo.serviceName}.${serviceInfo.serviceType}local.",
            "txt" to txtRecords,
            "host" to hostString,
            "port" to port.toString(),
            "address" to hostString
        )
    }

    private fun getDeviceKey(serviceInfo: NsdServiceInfo): String {
        return "${serviceInfo.serviceName}.${serviceInfo.serviceType}"
    }

    private fun isSameDevice(lhs: Map<String, Any?>?, rhs: Map<String, Any?>?): Boolean {
        if (lhs == null || rhs == null) return false
        val lName = lhs["fullName"] as? String
        val rName = rhs["fullName"] as? String
        return lName != null && rName != null && lName == rName
    }

    private fun stopScanInternal() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                // Already stopped or not started
            }
        }
        discoveryListener = null
        discoveredDevices.clear()
        resolvedDevices.clear()
        isScanning = false
    }

    private fun stopPublishInternal() {
        nsdManager?.unregisterService(registrationListener)
        isPublishing = false
    }
}