Post room (Android)
===================

This is Post room (companion app) for Android. The app is, per request, a thin wrapper around a web application implemented using the CustomTabs framework. Some native functionality is implemented for reading university MIFARE cards.

It is used by estates staff to process received parcels and aid in picking up items for residents calling in at the post room.

The app is written in Kotlin.

Configuring the instance
------------------------

For local development, use the app's preferences activity (accessed via the action bar's settings item - indicated with a cog) and set the instance URL to:

```
https://$hostname.warwick.ac.uk/
```
