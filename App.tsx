/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import {PermissionsAndroid, Platform, NativeModules} from 'react-native';

function App() {
  const {ZebraScanner} = NativeModules;

  async function requestBluetoothPermissions() {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        ]);
        const allPermissionsGranted = Object.values(granted).every(
          permission => permission === PermissionsAndroid.RESULTS.GRANTED,
        );
        if (allPermissionsGranted) {
          console.log('Bluetooth permission granted');
          // Initialize scanner here if needed
          initializeScanner();
        } else {
          console.log('Bluetooth permission denied');
        }
      } catch (err) {
        console.warn(err);
      }
    } else {
      // For iOS, handle permissions if necessary
      console.log(
        'iOS does not require Bluetooth permissions in the same way.',
      );
    }
  }

  const initializeScanner = () => {
    // Call the method to start the scanner
    ZebraScanner.setEnabled(true); // Ensure your setEnabled method is working
  };

  React.useEffect(() => {
    requestBluetoothPermissions();
  }, []);

  return <></>;
}

export default App;
