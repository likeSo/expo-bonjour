import {
  AndroidConfig,
  ConfigPlugin,
  withAndroidManifest,
  withInfoPlist,
} from "expo/config-plugins";

const withExpoBonjourConfig: ConfigPlugin<{
  localNetworkUsageDescription?: string;
  bonjourServices?: string[];
}> = (config, props) => {
  config = withInfoPlist(config, (c) => {
    if (props.localNetworkUsageDescription) {
      c.modResults["NSLocalNetworkUsageDescription"] =
        props.localNetworkUsageDescription;
    }
    if (props.bonjourServices) {
      c.modResults["NSBonjourServices"] = props.bonjourServices;
    }
    return c;
  });

  config = withAndroidManifest(config, (c) => {
    const permissions = [
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE",
      "android.permission.CHANGE_WIFI_MULTICAST_STATE",
      "android.permission.NEARBY_WIFI_DEVICES",
      "android.permission.ACCESS_WIFI_STATE",
    ];
    if (!c.modResults.manifest["uses-permission"]) {
      c.modResults.manifest["uses-permission"] = [];
    }
    permissions.forEach((permission) => {
      if (!isPermissionExists(c.modResults.manifest["uses-permission"], permission)) {
        c.modResults.manifest["uses-permission"]?.push({
          $: {
            "android:name": permission,
          },
        });
      }
    });
    return c;
  });

  return config;
};

export default withExpoBonjourConfig;

type ManifestPermission = {
  $: {
    "android:name": string;
  };
};

const isPermissionExists = (
  permissions: ManifestPermission[] | undefined,
  permissionName: string
) => {
  if (!permissions) {
    return false;
  }
  return permissions.some((p) => p.$["android:name"] === permissionName);
};
