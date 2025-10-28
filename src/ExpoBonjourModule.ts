import { NativeModule, requireNativeModule } from "expo";

import {
  AnyServiceType,
  ExpoBonjourModuleEvents,
  KnownServiceTypes,
  ServiceDevice,
  ServiceDeviceWithHost,
  ServiceProtocol,
} from "./ExpoBonjour.types";

declare class ExpoBonjourModule extends NativeModule<ExpoBonjourModuleEvents> {
  startScan(
    service: KnownServiceTypes | AnyServiceType,
    protocol: ServiceProtocol,
    domain?: string | null
  ): Promise<void>;
  stopScan(): Promise<void>;

  getAllDevices(): Promise<ServiceDevice[]>;

  getIPAddress(device: ServiceDevice): Promise<ServiceDeviceWithHost>
}

export default requireNativeModule<ExpoBonjourModule>("ExpoBonjour");
