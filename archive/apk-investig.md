# APK Investigation: enjpower-bms-4.17.0.175-arm64-v8a.apk

## Summary

The app is **Bluetooth-only**. There is no USB serial, UART, or OTG support for phone-to-BMS wired communication.

---

## App Identity

| Field | Value |
|---|---|
| Package | `com.enjpower.bms` |
| Version | 4.17.0 (versionCode 175) |
| Framework | Qt 6 / QML with native C++ |
| Target ABI | arm64-v8a |
| Main native library | `libenjpower_arm64-v8a.so` |
| Assets | `android_rcc_bundle.rcc` (Qt resource bundle) |

---

## Communication Layer

The app uses a dedicated BLE library package `com.smartsoft.ble`:

| Class | Role |
|---|---|
| `com.smartsoft.ble.BleService` | The sole communication service |
| `com.smartsoft.ble.Bluetooth` | BLE connection management |
| `com.smartsoft.ble.BluetoothInfo` | Device info container |
| `com.smartsoft.ble.JBluetoothLeUtils` | BLE utilities |
| `com.smartsoft.ble.JBluetoothUuid` | BLE UUID definitions |
| `com.smartsoft.ble.JScanRecord` | BLE scan record parsing |
| `com.smartsoft.ble.BleType` | BLE type enum |
| `com.smartsoft.ble.ConnectState` | Connection state tracking |
| `com.smartsoft.ble.DeviceState` | Device state tracking |
| `com.smartsoft.ble.NativeClass` | JNI bridge to native Qt code |
| `com.smartsoft.ble.MainActivity` | Main Android activity |

Native-side BLE functions:

- `JBleChannel::startScan`
- `Transfer::startScan`
- `JMain::setBluetoothName` / `JMain::bluetoothName`
- `JMain::setBluetoothAddr` / `JMain::bluetoothAddr`
- `JMain::setBluetoothPwd` / `JMain::bluetoothPwd`

---

## AndroidManifest Analysis

### Declared permissions

| Permission | Purpose |
|---|---|
| `android.permission.BLUETOOTH` | BLE communication |
| `android.permission.BLUETOOTH_ADMIN` | BLE management |
| `android.permission.BLUETOOTH_SCAN` | BLE scanning (Android 12+) |
| `android.permission.BLUETOOTH_CONNECT` | BLE connection (Android 12+) |
| `android.permission.BLUETOOTH_ADVERTISE` | BLE advertising |
| `android.permission.ACCESS_FINE_LOCATION` | Required for BLE scan on older Android |
| `android.permission.ACCESS_COARSE_LOCATION` | Required for BLE scan on older Android |
| `android.permission.ACCESS_BACKGROUND_LOCATION` | Background BLE |
| `android.permission.INTERNET` | Network access |
| `android.permission.ACCESS_NETWORK_STATE` | Network state |
| `android.permission.WRITE_EXTERNAL_STORAGE` | File storage |
| `android.permission.SYSTEM_ALERT_WINDOW` | Overlay windows |

### Declared features

| Feature | Required |
|---|---|
| `android.hardware.bluetooth_le` | Yes |

### Notable absences

| Feature / Permission | Present? |
|---|---|
| `android.hardware.usb.host` | **No** |
| Any USB permission | **No** |
| Any serial / OTG permission | **No** |

---

## USB / Serial / OTG Evidence

Searched both `classes.dex` and `libenjpower_arm64-v8a.so` for:

- `UsbManager`, `UsbDevice`, `UsbAccessory`
- `android.hardware.usb`
- `ACTION_USB_DEVICE_ATTACHED`, `ACTION_USB_DEVICE_DETACHED`
- `usbserial`, `serialport`, `SerialPort`
- `FTDI`, `CH340`, `CP210`
- `cdc_acm`, `tty`, `comport`

**Result: Zero matches.** The app has no USB host API integration whatsoever.

---

## UART Protocol Strings — Context

The app contains strings and functions referencing UART and CAN:

- `"Select UART Protocol"`
- `"Select CAN Protocol"`
- `sendUart1ProtoNo`
- `sendUart2ProtoNo`
- `Transfer::loadGlobalProtocol`
- Protocol JSON resources: `resource/protocol/en_US.jsonds`, `resource/protocol/global-en_US.jsonds`

**These are NOT about the phone connecting via UART.** They configure the BMS board's own physical communication ports (uart1, uart2, CAN) — what protocol the BMS speaks on those external interfaces. These settings are sent to the BMS over the existing BLE connection.

---

## UI Connection Flow

The QML UI only exposes a Bluetooth workflow:

- `window.startScan(true, true)` — scan for BLE devices
- `window.disconnectDevice()` — disconnect BLE
- `bluetoothName` property — shows connected BLE device name
- `bluetoothPwd` — BLE connection password
- Image resource: `ble_disconnect.png`

There is no UI element, setting, or workflow for USB, serial, OTG, or any wired connection method.

---

## Conclusion for implement-test.md

This confirms the **"most likely outcome"** from Phase 5, steps 19-20 of the test plan:

> Phone-over-cable is blocked by app support, not by raw hardware capability.

### Practical paths forward

1. **Path A (recommended):** Use Bluetooth as intended — the app works fine for this.
2. **Path B:** Test wired UART from the Windows PC first using the desktop EXE (`jkbms.com-monitor-3.4.0-setup.exe`), per Phase 4 of the test plan.
3. **Path C (advanced):** If phone-over-cable is required, you would need to:
   - Build a custom Android app with USB serial support (e.g., using `usb-serial-for-android` library) that speaks the JK BMS serial protocol.
   - Use a generic Android serial terminal app to send raw protocol commands — requires reverse-engineering the JK BMS serial protocol first.
   - Modify this APK to add USB serial support — significant effort since it is a Qt/QML app with protocol logic baked into the compiled native library `libenjpower_arm64-v8a.so`.
