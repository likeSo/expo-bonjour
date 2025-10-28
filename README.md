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
  }, [newDevice]);
};
```

安卓部分还有一部分代码没有完全实现，README还需要完善