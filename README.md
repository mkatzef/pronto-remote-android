# pronto-remote-android

An Android app to send and record remote control commands through an Arduino device. Please see [this project](https://github.com/mkatzef/pronto-remote) for the Arduino setup.

## Getting Started

The app is not currently available through standard app distribution sources, it must be built and installed locally.

This project was created and uploaded to GitHub through [Android Studio](https://developer.android.com/tools). After cloning this repository, the project may be opened in, modified, and installed on an Android device, all using this IDE.

### Prerequisites

* Android Studio - "The Official IDE for Android". Available [here](https://developer.android.com/tools).
* An Arduino Device - The device capable of receiving and transmitting infrared signals in the Pronto Hex format\*.
* USB Cable and Adapter - A connection between the Android and Arduino devices allowing power and data to be transferred\*\*.

\* A circuit description and the compatible Arduino sketch are available [here](https://github.com/mkatzef/pronto-remote).

\*\* An example cable/adapter combination is:
* `USB C` to `USB A` On-the-go (OTG) adapter - Allowing `USB A` devices to connect to the Android device.
* `USB A` to `USB Mini B` cable - Allowing the Arduino device to connect to devices with `USB A` ports.

### Installation

As mentioned in the "Getting Started" section, to install this application, the source code must first be locally compiled using Android Studio. A description of this procedure may be found in [this android developer documentation](https://developer.android.com/training/basics/firstapp/running-app.html).

### Use

Once installed on the Android device, the application "Pronto Remote" should open automatically when a compatible Arduino device\* is connected (and permission is given). If the application is opened before a device is connected, tapping on the application's status message will attempt to connect with the device.

Once a connection is established, tapping on any of the 30\*\* remote buttons will send the corresponding command (through the Arduino device).

All communication between the Android and Arduino devices occurs over serial (as described in the Arduino Pronto Remote project) where the Android device assumes the role of the host.

Tapping on a button "selects" that button to be the destination for any newly recorded code, or to be edited through the edit menu. These features may be respectively achieved by tapping the "RECORD" and "EDIT" buttons.

Tapping the "RECORD" button waits for the Arduino device to record a new command. After tapping this button, an infrared controller should be aimed at the Arduino device's IR receiver and one of the controller's buttons should be pressed. While waiting for a response from the Arduino device, pressing another Pronto Remote button will cancel recording and send the corresponding code.

Tapping the "EDIT" button opens a menu allowing the button's device name and function to be specified, as well as manual Pronto Hex entry.

The "SAVE" and "IMPORT" buttons respectively store, and revert to a saved snapshot of the Pronto Remote layout. "SAVE" copies a description of Pronto Remote's buttons to the Android device's clipboard (to be pasted elsewhere). "IMPORT" opens a menu allowing a saved layout to be pasted and used to update the remote.

\* Compatible devices are any connected device with a Device ID of 1027.

\*\* The current version of the Pronto Remote Android app provides 30 user-programmable buttons. These are, regrettably, hard-coded into the `acitivty_main.xml` activity layout, using output from a Python script (not included).

## Authors

* **Marc Katzef** - [mkatzef](https://github.com/mkatzef)

## Acknowledgements

* **felHR85** (https://github.com/felHR85), author of the library [`UsbSerial`](https://github.com/felHR85/UsbSerial) which powers this project.

* **romannurik** (https://github.com/romannurik), author of the online tool [`Android Asset Studio`](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html), used to generate this application's icon.