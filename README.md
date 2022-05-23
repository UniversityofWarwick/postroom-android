Post room (Android)
===================

This is Post room (companion app) for Android. It is used by estates staff to process received parcels and aid in picking up items for residents calling in at the post room. It provides a fully-native experience for scanning-in parcels and a hybrid Custom Tabs implementation for collecting parcels, providing a wrapper around a Vue.js web application.

The app includes integration with Google's ML Kit for 100% on-device optical character recognition (for automatically reading parcel labels) and barcode recognition. It also includes built-in support for reading the University's MIFARE based ID cards for secure parcel collections

The app is written in Kotlin.

To learn more about the system, visit the [Warwick web page](https://warwick.ac.uk/services/its/servicessupport/web/postroom/).

Configuring the instance
------------------------

For local development, use the app's preferences activity (accessed via the action bar's settings item - indicated with a cog) and set the instance URL to:

```
https://$hostname.warwick.ac.uk/
```
