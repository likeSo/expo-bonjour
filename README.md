# expo-bonjour

Zero-config mDNS (Bonjour) service discovery for Expo and React Native apps.
一个”零配置“（mDNS）服务插件，让你可以利用mDNS和Bonjour服务，发现局域网中的设备。

# 安装

```bash
npx expo install expo-bonjour
```

> 注意，不支持在 Expo Go 中使用。请安装`expo-dev-client`并重新构建应用。

# 配置

在`app.json`中添加以下配置：

```json
{
  "expo": {
    "plugins": [
      [
        "expo-bonjour",
        {
          "localNetworkUsageDescription": "iOS上的本地局域网的权限说明文案",
          "bonjourServices": ["_http._tcp", "_https._tcp"]
        }
      ]
    ]
  }
}
```

# 使用

## 发现设备

```tsx
import ExpoBonjour from "expo-bonjour";
import { useEvent } from "expo";

const App = () => {
  const newDevice = useEvent(ExpoBonjour, "onDeviceFound");
  useEffect(() => {
    ExpoBonjour.startScan("_http", "_tcp");
    return () => {
      ExpoBonjour.stopScan();
    };
  }, []);

  useEffect(() => {
    if (newDevice) {
      console.log("发现设备:", newDevice);
    }

    /// 或者，你也可以通过监听onDeviceFound以及onDeviceRemoved事件，并通过getAllDevices()方法来获取当前最新的已经扫描到的所有设备。
  }, [newDevice]);
};
```

## 获取设备的IP地址

```tsx
import ExpoBonjour from "expo-bonjour";

const App = () => {
  const newDevice = useEvent(ExpoBonjour, "onDeviceFound");
  useEffect(() => {
    ExpoBonjour.startScan("_http", "_tcp");
    return () => {
      ExpoBonjour.stopScan();
    };
  }, []);

  useEffect(() => {
    if (newDevice) {
      ExpoBonjour.getIPAddress(newDevice).then((deviceInfoWithIPAddress) => {
        console.log("包含IP地址的设备详细信息:", deviceInfoWithIPAddress);
      });
    }
  }, [newDevice]);
};
```

## 发布服务

上述内容是扫描其他设备的信息的，你也可以把自己的信息发布到局域网内。需要注意的是，发布服务后，后续的接口处理需要自己额外处理。假如你在8080端口发布了一个_https._tcp的服务，那么你要自己处理8080端口收到的请求。

```tsx
import ExpoBonjour from "expo-bonjour";

const App = () => {
  useEffect(() => {
    ExpoBonjour.publish({
      name: "_myapp",
      port: 8080,
      service: "_tcp",
      txtRecord: {
        version: "1.0.0",
      },
    });
    return () => {
      ExpoBonjour.stopPublish();
    };
  }, []);
};
```


# 支持

本人热衷于开发各种Expo插件，热衷于学习与跟进Expo的最新动态。如果您有任何问题或建议，欢迎联系我。
QQ群：682911244