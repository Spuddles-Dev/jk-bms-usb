# implement-test.md

## Goal

Determine whether the **standard JK-B2A20S20P** can be used from a phone without Bluetooth by using the wired interface, assuming you already have:

- the **APK** for the mobile app
- the **Windows setup EXE** for the desktop software

This plan assumes the board is the **standard edition** for now, which means:

- **Bluetooth**: standard
- **UART**: standard
- **RS485**: optional
- **CAN**: optional

So for the standard unit, the only wired path worth testing first is **UART**, not RS485 or CAN.

---

## Phase 1 — Confirm the exact hardware variant

### 1. Record the exact label on the BMS
Photograph or write down:

- full model number on the unit
- any suffix such as:
  - `P`
  - `PR`
  - `PC`
  - `PH`
  - `PHR`
  - `PHC`
  - `PHCR`

### 2. Inspect the communication connectors
From the manual, check whether the unit exposes:

- **P4** communication interface:
  - CAN_L
  - CAN_H
  - RS485_A
  - RS485_B
- **P5** UART interface:
  - GND
  - RX
  - TX
  - VGPS

If this is truly the standard model, treat **P5 UART** as the primary wired test path.

### 3. Do not assume the cable type from marketplace listings
Separate these clearly:

- **UART / TTL serial**
- **RS485**
- **CAN**

These are different electrical interfaces and are **not interchangeable**.

---

## Phase 2 — Build a safe test setup

### 4. Prepare a bench-safe environment
Before software testing:

- make sure the BMS is correctly wired and activated
- avoid testing on a live vehicle/system if possible
- use a stable battery pack and charger
- keep notes on every change you make

### 5. Get the right adapter for the standard model
For a standard board, start with:

- **USB to 3.3V UART adapter**
- ideally based on a common chip such as:
  - FTDI
  - CP2102
  - CH340

Avoid:

- 5V TTL unless you have confirmed the port is 5V tolerant
- RS485 adapter for the first standard-model test
- USB-CAN adapter for the first standard-model test

### 6. Wire the UART connection carefully
Match signals correctly:

- adapter **GND** -> BMS **GND**
- adapter **TX** -> BMS **RX**
- adapter **RX** -> BMS **TX**

Do **not** connect voltage/power lines casually.

Treat **VGPS** as a power output/special-purpose line and leave it disconnected unless documentation explicitly says otherwise.

### 7. Keep a recovery path
Before changing any settings:

- take screenshots of defaults in the app/software
- record battery chemistry and cell count
- record existing protection thresholds if readable

---

## Phase 3 — Establish a baseline with official Bluetooth

### 8. Install the APK on an Android phone
Use the APK first because Bluetooth is the officially documented mobile path.

### 9. Connect over Bluetooth and confirm normal operation
Verify that the app can:

- discover the BMS
- read pack voltage/current
- read cell voltages
- show temperatures
- show or edit configuration pages

This gives you a known-good baseline before testing wired methods.

### 10. Note the Android permission behavior
If the app asks for Bluetooth and location/GPS permission, record that.

This does **not** prove the BMS needs GPS for function. It may just be the app’s BLE scanning requirement.

---

## Phase 4 — Test the Windows software first

### 11. Install the setup EXE on a Windows PC
Use the PC first because desktop software is more likely to support USB serial than the phone app.

### 12. Connect the UART adapter to Windows
Then:

- open Device Manager
- identify the COM port number
- install adapter drivers if needed

### 13. Connect the adapter to the BMS UART port
Use only the three signal connections:

- GND
- RX
- TX

### 14. Start with read-only testing
Open the JK desktop software and try to:

- detect the device
- select the COM port
- read live values
- read configuration
- avoid writing settings yet

### 15. Log the result
Record:

- whether the software detects the BMS
- which COM settings were required, if any
- whether data is stable or garbled
- whether the software expects a password, address, or protocol mode

### Decision point
If **Windows over UART fails**, stop and reassess before trying phone-over-cable.

Possible reasons:

- wrong hardware variant
- wrong port
- wrong adapter voltage level
- wrong driver
- wrong protocol/settings
- desktop software may expect RS485 on some models

---

## Phase 5 — Determine whether phone-over-cable is realistic

### 16. Only proceed after PC success
Do not jump straight to the phone cable test unless the PC has already communicated successfully over the same interface.

If the PC cannot talk to the BMS over that adapter/port, the phone is even less likely to work.

### 17. Check the Android phone for USB OTG support
Confirm the phone can operate as a USB host.

You need:

- Android phone
- USB OTG support
- correct physical adapter:
  - USB-C OTG or micro-USB OTG, depending on the phone

### 18. Connect the same working UART adapter to the phone
Use the exact hardware chain that worked on Windows.

### 19. Determine whether the APK supports USB serial
Check whether the APK:

- has any setting for:
  - USB
  - serial
  - COM/TTY device
  - UART
  - OTG
- or whether it is Bluetooth-only

If the app only presents Bluetooth workflows, that is the main blocker.

### 20. If the APK is Bluetooth-only, choose one of two paths

#### Path A — Use Bluetooth as intended
This is the practical route if the APK has no wired serial support.

#### Path B — Reverse/test with a generic serial tool
Only do this if you are comfortable experimenting.

Use an Android serial terminal or USB serial test app to check whether:

- the phone sees the USB-UART adapter
- serial data can be read at all
- the protocol appears active

This may show raw bytes only and may still be useless without the JK protocol.

---

## Phase 6 — If the PC software does not work over UART

### 21. Re-check whether the board may actually be a comms variant
If the desktop EXE seems to expect RS485 or CAN, revisit the model suffix and ports.

The standard family supports optional variants, so a seller-supplied cable may be intended for a **different version** than the one in hand.

### 22. Test with the seller’s cable only after identifying its interface
Before using the cable:

- identify whether it is:
  - USB-UART
  - USB-RS485
  - USB-CAN

Do not trust mixed listing language alone.

### 23. If the cable is RS485
That only makes sense if the board actually has the **RS485 option** enabled and exposed.

### 24. If the cable is CAN
That only makes sense if the board is the **CAN variant** and you have software that supports JK CAN communication.

---

## Phase 7 — Safe configuration practice

### 25. Avoid writing settings early
Do not change:

- chemistry
- cell count
- current limits
- temperature thresholds
- address/protocol mode

until you have a stable read connection and a backup record of current values.

### 26. Change only one thing at a time
If you must test writes:

- change a non-critical setting first
- confirm it writes correctly
- confirm it reads back correctly
- confirm normal operation afterward

### 27. Keep a rollback record
Maintain a table with:

- parameter name
- original value
- test value
- result
- restored value

---

## Expected outcomes

### Best-case outcome
- Bluetooth app works on phone
- Windows software works over UART
- Android phone also works over USB-UART and OTG with compatible software

### Most likely outcome
- Bluetooth app works on phone
- Windows software works over a wired adapter
- phone-over-cable is blocked by app support, not by raw hardware capability

### Failure outcome
- no wired comms work because:
  - wrong board variant
  - wrong port
  - wrong cable
  - wrong voltage/interface
  - seller mixed RS485/CAN/UART terminology

---

## Recommended order of execution

1. Confirm exact model suffix on the unit
2. Install APK and verify Bluetooth operation
3. Install Windows EXE
4. Test **PC -> USB to 3.3V UART -> P5 UART**
5. Only after PC success, test **Android -> OTG -> same UART adapter**
6. If that fails, assume the mobile app is Bluetooth-only unless proven otherwise

---

## Practical go/no-go rule

For a **standard JK-B2A20S20P**, do **not** start by buying or testing RS485 or CAN hardware.

Start with:

- Bluetooth on phone
- UART on PC
- then UART on Android only if PC proves the path works

That is the lowest-risk and most technically consistent path.
