export type AnyServiceType = `_${string}`;
export type KnownServiceTypes =
  | "_http"
  | "_https"
  | "_ftp"
  | "_sftp"
  | "_smb"
  | "_afpovertcp"
  | "_nfs"
  | "_webdav"
  | "_webdavs"
  | "_ipp"
  | "_printer"
  | "_pdl-datastream"
  | "_airprint"
  | "_airplay"
  | "_raop"
  | "_daap"
  | "_hap"
  | "_spotify-connect"
  | "_dlna"
  | "_ssh"
  | "_telnet"
  | "_rfb"
  | "_rdp"
  | "_workstation"
  | "_homekit"
  | "_hue"
  | "_miio"
  | "_ichat"
  | "_xmpp-client"
  | "_sip"
  | "_minecraft"
  | "_steam"
  | "_xbox"
  | "_adb"
  | "_scanner"
  | "_touch-able"
  | "_sleep-proxy";

export type ServiceProtocol = "_tcp" | "_udp";

export type ServiceDevice = {
  name: string;
  fullName: string;
  address: string;
  txt: Record<string, string>;
};

export type ServiceDeviceWithHost = ServiceDevice & {
  host?: string;
  port?: string;
};

export type ExpoBonjourModuleEvents = {
  onStartScan: () => void;
  onScanError: (payload: { errorCode: number }) => void;
  onStopScan: () => void;

  onDeviceFound: (payload: ServiceDevice) => void;
  onDeviceChange: (payload: ServiceDevice) => void;
  onDeviceRemoved: (payload: ServiceDevice) => void;
};

export type PublishingOptions = {
  name: string;
  port: number;
  service: KnownServiceTypes | AnyServiceType;
  domain?: string | null;
  txtRecord: Record<string, string>;
};
