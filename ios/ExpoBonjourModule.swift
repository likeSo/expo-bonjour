import ExpoModulesCore
import Network
import dnssd

public class ExpoBonjourModule: Module {
    var browser: NWBrowser?
    var listener: NWListener?
    var isScanning = false
    var isPublishing = false
    
    
  public func definition() -> ModuleDefinition {
      Name("ExpoBonjour")
      
      Events("onStartScan", "onStopScan", "onDeviceFound", "onDeviceChange", "onDeviceRemoved", "onPublishingError")
      
      AsyncFunction("startScan") { (serviceType: String, `protocol`: String, domain: String?) in
          if isScanning {
              stopScanInternal()
          }
          let type = "\(serviceType).\(`protocol`)"
          let descriptor = NWBrowser.Descriptor.bonjourWithTXTRecord(type: type, domain: domain)
          let parameters = NWParameters()
          parameters.includePeerToPeer = true
          browser = NWBrowser(for: descriptor, using: parameters)
          
          browser?.stateUpdateHandler = { [weak self] newState in
              switch newState {
              case .ready:
                  self?.sendEvent("onStartScan")
              case .failed(let error):
                  self?.browser?.cancel()
                  let payload: [String: Any]
                  switch error {
                  case .posix(let errorCode):
                      payload = ["errorCode": errorCode.rawValue, "type": "POSIX ERROR"]
                  case .dns(let type):
                      payload = ["errorCode": type, "type": "DNS ERROR"]
                  case .tls(let status):
                      payload = ["errorCode": status, "type": "TLS ERROR"]
                  @unknown default: payload = [:]
                      
                  }
                  self?.sendEvent("onError", payload)
              case .cancelled:
                  self?.isScanning = false
                  self?.browser = nil
                  self?.sendEvent("onStopScan")
              default: break;
              }
          }
          
          browser?.browseResultsChangedHandler = { [weak self] results, changes in
              for change in changes {
                  switch change {
                  case .added(let result):
                      self?.sendEvent("onDeviceFound", parseNWBrowserResult(result))
                  case .changed(old: _, new: let result, flags: _):
                      self?.sendEvent("onDeviceChange", parseNWBrowserResult(result))
                  case .removed(let result):
                      self?.sendEvent("onDeviceRemoved", parseNWBrowserResult(result))
                  default:
                      break
                  }
              }
          }
          isScanning = true
          browser?.start(queue: .global())
      }
      
      AsyncFunction("getAllDevices") {
          if browser != nil {
              if let results = self.browser?.browseResults, !results.isEmpty {
                  return results.map{parseNWBrowserResult($0)}
              } else {
                  return []
              }
          } else {
              throw Exception(name: "ERR_NO_BROWSER", description: "Please call startScan() first.")
          }
      }
      
      AsyncFunction("stopScan") {
          stopScanInternal()
      }
      
      AsyncFunction("getIPAddress") { (device: [String: Any], promise: Promise) in
          if let browser = self.browser, !browser.browseResults.isEmpty {
              let targetEndpoint = browser.browseResults.first { element in
                  let elementDictionary = parseNWBrowserResult(element)
                  if isSameEndpoint(elementDictionary, device) {
                      return true
                  }
                  return false
              }
              if targetEndpoint != nil {
                  let connection = NWConnection(to: targetEndpoint!.endpoint, using: .udp)
                  connection.stateUpdateHandler = { state in
                      if case .ready = state, let endpoint = connection.currentPath?.remoteEndpoint, case .hostPort(let host, let port) = endpoint {
                          let hostString = getCleanHostAddress(host)
                          var newDeviceInfo = device
                          newDeviceInfo["host"] = hostString ?? "\(host)"
                          newDeviceInfo["port"] = "\(port.rawValue)"
                          connection.cancel()
                          promise.resolve(newDeviceInfo)
                      } else if case .failed(let error) = state {
                          promise.reject(Exception(name: "ERR_CANNOT_GET", description: error.localizedDescription))
                      }
                  }
                  
                  connection.start(queue: .global())
              } else {
                  promise.reject(Exception(name: "ERR_NO_DEVICE_FOUND", description: "No target device found, please make sure your device info is the latest."))
              }
          } else {
              promise.reject(Exception(name: "ERR_NO_BROWSER_OR_DEVICE",
                                       description: "No browser or devices now. Please make sure call startScan() first and at least 1 device found."))
          }
      }
      
      
      AsyncFunction("startPublish") { (options: PublishingOptions) in
          if isPublishing {
              listener?.cancel()
          }
          do {
              let parameters = NWParameters.tcp
              parameters.allowLocalEndpointReuse = true
            listener = try NWListener(using: parameters, on: NWEndpoint.Port(rawValue: options.port)!)
              var txtRecord: Data?
              if options.txtRecord != nil, !options.txtRecord!.isEmpty {
                  let dataPairs = options.txtRecord!.compactMapValues{$0.data(using: .utf8)}
                  txtRecord = NetService.data(fromTXTRecord: dataPairs)
              }
              listener?.service = NWListener.Service(name: options.name, type: options.service, domain: options.domain, txtRecord: txtRecord)
              
              listener?.stateUpdateHandler = { [weak self] state in
                  switch state {
                  case .failed(let error):
                      let payload: [String: Any]
                      switch error {
                      case .posix(let errorCode):
                          payload = ["errorCode": errorCode.rawValue, "type": "POSIX ERROR"]
                      case .dns(let type):
                          payload = ["errorCode": type, "type": "DNS ERROR"]
                      case .tls(let status):
                          payload = ["errorCode": status, "type": "TLS ERROR"]
                      @unknown default: payload = [:]
                          
                      }
                      self?.sendEvent("onPublishingError", payload)
                      self?.listener?.cancel()
                  default: break
                  }
              }
              
              listener?.newConnectionHandler = { [weak self] connection in
                  switch connection.endpoint {
                  case let .hostPort(host, port):
                      switch host {
                      case .ipv4(let address):
                          self?.sendEvent("onNewConnectionInComing", ["host": address.rawValue.toIPv4String(), "port": "\(port.rawValue)"])
                      case .ipv6(let address):
                          self?.sendEvent("onNewConnectionInComing", ["host": address.rawValue.toIPv6String(), "port": "\(port.rawValue)"])
                      default:
                          break
                      }
                  default: break
                  }
              }
              
              listener?.start(queue: .global())
          } catch {
              throw Exception(name: "ERR_CANNOT_NWListener", description: "Cannot create NWListener instance.").causedBy(error)
          }
      }
      
      AsyncFunction("stopPublishing") {
          listener?.cancel()
          listener = nil
      }
  }
    
    func stopScanInternal() {
        browser?.cancel()
        browser = nil
        isScanning = false
    }
}


func parseNWBrowserResult(_ result: NWBrowser.Result) -> [String: Any] {
    let endpoint = result.endpoint
    var resultDict: [String: Any] = [:]
    
    if case .bonjour(let txtRecord) = result.metadata {
        resultDict["txt"] = txtRecord.dictionary
    }
    
    switch endpoint {
    case let .service(name, type, domain, _):
        resultDict["name"] = name
        resultDict["type"] = type
        resultDict["domain"] = domain
        resultDict["fullName"] = "\(name).\(type).\(domain)"
        
    case let .hostPort(host, port):
        switch host {
        case .ipv4(let address):
            resultDict["address"] = "\(address)"
            resultDict["port"] = port.rawValue
        case .ipv6(let address):
            resultDict["address"] = "\(address)"
            resultDict["port"] = port.rawValue
        case .name(let hostName, _):
            resultDict["address"] = hostName
            resultDict["port"] = port.rawValue
        @unknown default:
            break
        }
    case .unix(let path):
        resultDict["unixPath"] = path
    case let .url(url):
        resultDict["url"] = url.absoluteString
        
    default:
        resultDict["name"] = ""
        resultDict["fullName"] = ""
        resultDict["txt"] = [:]
        resultDict["address"] = ""
        resultDict["port"] = 0
    }
    
    return resultDict
}


func isSameEndpoint(_ lhs: [String: Any], _ rhs: [String: Any]) -> Bool {
    if let lName = lhs["fullName"] as? String, let rName = rhs["fullName"] as? String, lName == rName {
        return true
    }
    return false
}


func getCleanHostAddress(_ host: NWEndpoint.Host) -> String? {
    switch host {
    case .ipv4(let address):
        return address.rawValue.toIPv4String()
    case .ipv6(let address):
        return address.rawValue.toIPv6String()
    case .name(let name, _):
        return name
    default: return nil
    }
}


extension Data {
    func toIPv4String() -> String? {
        guard self.count == 4 else { return nil }
        return self.map { String($0) }.joined(separator: ".")
    }
    
    func toIPv6String() -> String? {
            guard count == 16 else { return nil }
            
            var result: [String] = []
            for i in stride(from: 0, to: count, by: 2) {
                let byte1 = self[i]
                let byte2 = self[i + 1]
                let value = (UInt16(byte1) << 8) | UInt16(byte2)
                result.append(String(format: "%x", value))
            }
            
            return result.joined(separator: ":")
        }
}
