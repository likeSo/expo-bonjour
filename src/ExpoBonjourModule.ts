import { NativeModule, requireNativeModule } from "expo";

import {
  AnyServiceType,
  ExpoBonjourModuleEvents,
  KnownServiceTypes,
  PublishingOptions,
  ServiceDevice,
  ServiceDeviceWithHost,
  ServiceProtocol,
} from "./ExpoBonjour.types";

declare class ExpoBonjourModule extends NativeModule<ExpoBonjourModuleEvents> {
  /**
   * 开始扫描局域网mDNS服务。Starts the mDNS scan.
   * @param service 服务类型。比如_https. The service type to scan for.
   * @param protocol 服务协议。比如_tcp. The service protocol to scan for.
   * @param domain 域名。默认是"local". The domain to scan for. Defaults to "local".
   */
  startScan(
    service: KnownServiceTypes | AnyServiceType,
    protocol: ServiceProtocol,
    domain?: string | null
  ): Promise<void>;
  /**
   * 停止扫描。Stops the mDNS scan.
   */
  stopScan(): Promise<void>;
  /**
   * 获取所有扫描到的设备。Gets all devices found during the scan.
   */
  getAllDevices(): Promise<ServiceDevice[]>;
  /**
   * 获取设备的IP地址。Gets the IP address of a device.
   * @param device 设备信息，传入扫描到的设备。The device to get the IP address for.
   */
  getIPAddress(device: ServiceDevice): Promise<ServiceDeviceWithHost>
  /**
   * 开始发布服务。Starts publishing a service.
   * @param options 发布选项。The publishing options.
   */
  startPublish(options: PublishingOptions): Promise<void>;
  /**
   * 停止发布服务。Stops publishing a service.
   */
  stopPublish(): Promise<void>;
}

export default requireNativeModule<ExpoBonjourModule>("ExpoBonjour");
