import { useEvent } from "expo";
import ExpoBonjour, { ServiceDevice } from "expo-bonjour";
import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  FlatList,
  SafeAreaView,
  ScrollView,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from "react-native-safe-area-context";

export default function App() {
  return (
    <SafeAreaProvider>
      <AppContent />
    </SafeAreaProvider>
  );
}

const AppContent = () => {
  const insets = useSafeAreaInsets();
  const newDevice = useEvent(ExpoBonjour, "onDeviceFound");

  useEffect(() => {
    if (newDevice) {
      console.log(newDevice);
      setFoundDevices((prev) => [...prev, newDevice]);
    }
  }, [newDevice]);

  useEffect(() => {
    ExpoBonjour.startScan("_http", "_tcp");
  }, []);

  const [foundDevices, setFoundDevices] = useState<ServiceDevice[]>([]);

  const renderItem = useCallback(({ item }: { item: ServiceDevice }) => {
    return (
      <TouchableOpacity
        onPress={async () => {
          try {
            const fullInfo = await ExpoBonjour.getIPAddress(item);
            Alert.alert(JSON.stringify(fullInfo));
          } catch (e) {
            console.error(e);
          }
        }}
      >
        <Text>Name: {item.name}</Text>
        <Text>Fullname: {item.fullName}</Text>
        <Text>Address: {item.address}</Text>
      </TouchableOpacity>
    );
  }, []);

  return (
    <FlatList
      style={styles.container}
      contentContainerStyle={{
        paddingTop: insets.top,
        paddingLeft: insets.left,
        paddingRight: insets.right,
        paddingBottom: insets.bottom,
      }}
      data={foundDevices}
      renderItem={renderItem}
      keyExtractor={(item, index) => index.toString()}
    />
  );
};

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  view: {
    flex: 1,
    height: 200,
  },
};
