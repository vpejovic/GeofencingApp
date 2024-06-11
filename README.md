# Geofencing Application

## Introduction 

Geofence is a virtual area, corresponding to the actual geographic area. Detecting a user's entrance, exit or dwelling within a geofence enables a number of interesting location-based mobile applications. For example, geofences can be used to issue location-based reminders, we can use geofences to post location-based information, and we can also set geofences dynamically, thus detect when a person is close to one of her friends.
In Android, geofences are supported directly by Google Play Services - you simply have to define where the centre of the monitored area is, the radius of the circular area that you want to monitor, and the transition (enter, exit, dwell) that you want to monitor. 
This lab we are going to build an app with location-based reminders. The app will remind its users to:
1. go to the gym after work; 
2. once in the gym for some time (dwell), synchronize their smart wristbands; 
3. remotely turn the heating on, as they are getting closer to home. 

Three geofences, at work, gym, and home, will be used.

## Application scaffolding ##

### Loading project ###
We are going to implement a rather advanced application in a short period of time, thus, we won’t start from zero. In Android Studio open a new project from Version Control and type in https://github.com/vpejovic/GeofencingApp/

### Setting up Google Maps API Key ###
Our app is going to rely on Google Map tiles. Using this requires a Google Maps API key. Follow the instructions described [here](https://developers.google.com/maps/documentation/android-sdk/start) and set `google_maps_key` parameter in `google_maps_api.xml` file in `res` directory of your project to the value of your key.

### Checkpoint - Map shown ###
Ensure that the project compiles and runs on your emulator or phone. You should see a map (the actual location will depend on what you set in the emulator) with three movable icons, for work, home, and gym, on it. Test whether moving the icons works.

### Setting up the emulator ###
To test our app we are not going to run around the city to trigger geofences, but will use the emulator and mock locations provided through a pre-collected trace. First, download the gpx trace from [here](traces/ljubljana.gpx). Then, open the extended controls of the emulator (three dots) and put `46.05178` for latitude and `14.49968` for longitude and click `Send`. Then, open Google Maps on the emulator and click on My Location icon. This should set the emulator's location to Ljubljana, Slovenia. Next, back in the extended controls load the gpx trace you previously downloaded. Finally, open our Geofencing App and using the extended controls play the gpx trace. You should see the user's location moving through Ljubljana. 

### Brief Tutorial on Geofence in Android
Geofences are set through `GeofencingClient`. This class has a method `addGeofences` that takes the following arguments: 
* An instance of `GeofencingRequest` representing the definition of the geofence (e.g. it’s location, transition type monitored, etc.);
* An instance of `PendingIntent` referring to the `Intent` that will be called when the right transition to/from a geofence happens;

We want our app to send notifications when a user is entering/exiting a geofence defined by the location of the icons on the map. Thus, setting the `GeofenceRequest` should happen when a user moves an icon on the map. Further, the notification should be fired when the right event happens. We don’t know whether the user will have our application open at the time when this happens, thus, we should be prepared to send the notifications from the background. We can do that from a `BroadcastReceiver`. The receiver will be triggered by Google Play Services when the geofence-related transition happens and will then issue a notification to the user. 

In a nutshell:
* **MapsActivity:**
    * Instantiate `GeofencingClient`;
    * Set `GeofencingRequest` to define the geofences
    * Set `PendingIntent` to call `BroadcastReceiver` when a geofencing-related transition happens;
* **BroadcastReceiver:**
    * Get called when Google Play Services detect geofencing-related events;
    * Fire a notification with the appropriate text, depending on which geofence was triggered;
 
Make sure you understand the above before you proceed with programming.

## Programming the app ##

### Instantiate geofencing client ###
Open `MapsActivity` and instantiate `mGeofencingClient` in `onCreate`:
```Kotlin
mGeofencingClient = LocationServices.getGeofencingClient(this)
```

### Create geofencing request
Implement a private function `getGeofencingRequest` that takes a marker type (home, work, fitness) and the coordinates (latitude and longitude) of a marker, and returns a request for a specific geofence. The function should check which marker triggered the method and if the type equals to:
* `R.string.map_marker_home`: set a geofence with a circular region of 200m, with a transition type `Geofence.GEOFENCE_TRANSITION_ENTER`
* `R.string.map_marker_work`: set a geofence with a circular region of 300m, with a transition type `Geofence.GEOFENCE_TRANSITION_EXIT`
* `R.string.map_marker_fitness`: set a geofence with a circular region of 300m, with a transition type `Geofence.GEOFENCE_TRANSITION_DWELL`

To build the Geofences you should use something like (example for `home`):
```Kotlin
val geofence = with(Geofence.Builder()){
                setRequestId(type)
                setCircularRegion(lat, lon, 200F)
                setExpirationDuration(Geofence.NEVER_EXPIRE)
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                build()
            }
```

Exceptionally, for the dwell transition you should also set the delay before the action is triggered. This ensures that the notification is not triggered if a user simply passes by the gym. Use: `setLoiteringDelay(1000)`

Think how would you implement setting the geofence parameters for each of the three locations without having too much redundant code.

To build and return the `GeofenceRequest`, you just need to put the `Geofence` in a list and call the request builder:

```Kotlin
return with(GeofencingRequest.Builder()){
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(geofence))
            build()
```

### Define `PendingIntent` to be activated when geofence-transition happens ###
If conditions for one of the geofences are satisfied, an Intent will be fired to call `BroadcastReceiver`. We will define a `PendingIntent`, essentially granting the geofencing client the ability to execute a predefined intent. The `Intent` will be broadcasting to `GeofenceBroadcastReceiver`:
```Kotlin
private val mGeofencePendingIntent: PendingIntent by lazy {
  val intent = Intent(this,
    GeofenceBroadcastReceiver::class.java)

  PendingIntent.getBroadcast(this, 0, intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
}
```
Note the `lazy` keyword in the above excerpt - this tells the compiler that the initialisation should not happen immediately, when the `Activity` is created, but only the first time `mGeofencePendingIntent` is actually accessed. This saves resources, in case the property is never executed during the `Activity` lifecycle (e.g. a user opens an app and does not set any geofences). 

### Adding the geofence through the client
With the `Intent` prepared and geofences specified, we can tell our `GeofencingClient` to start observing the location and firing our `Intent` in case the geofence conditions are satisfied. 
In `addGeofence` function add:
```Kotlin
mGeofencingClient.addGeofences(request, mGeofencePendingIntent)
                .addOnCompleteListener(this)
```

### Connecting marker movement with geofence setting
Finally, uncomment the code in `setOnMarkerDragListener` to enable geofence setting when a marker is moved.

### Sending notifications
Open `GeofenceBroadcastReceiver`. This is a receiver that will be called when a geofencing-related event is detected. Check its `onReceive` method. Here we handle Intents that have triggered the receiver. We should check which geofence was triggered and what was the event. We should then send a different notification depending on what has happened. Most of the code is already written, but make sure you understand what each of the lines is for. 

What is missing, though, is the actual notification posting. In latest versions of Android SDK, notifications should be grouped in channels, which why we have already created one in `onReceive`. We should now define what happens when a user clicks on a notification - our `MapsActivity` should be opened. This `Activity` will be started by using *surprise* an `Intent`, more specifically a `PendingIntent`. We also need to define a few other properties of a notification, such as its title, an icon that will be shown, and so on. The full code is here:
```Kotlin
val notificationIntent = Intent(context, MapsActivity::class.java)
notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
val notifPendingIntent = PendingIntent.getActivity(
  context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

val newNotification = NotificationCompat.Builder(context, CHANNELID)
  .setSmallIcon(android.R.drawable.ic_dialog_info)
  .setContentTitle(notificationTitle)
  .setContentText(notificationDetails)
  .setContentIntent(notifPendingIntent)
  .setAutoCancel(true)
  .build();

with(NotificationManagerCompat.from(context)) {
  notify(NOTIFICATIONID, newNotification)
}
```

## Testing ##
Your app is now completed. Testing it will require you to move the markers around the map of Ljubljana and run the trace. If you set the work icon somewhere close to the Faculty of Computer and Information Science on Vecna pot, the home icon close to Kolodvor, and the fitness icon on Bleiweisova cesta you should see all three notifications, for leaving work, dwelling at the gym, and arriving home, triggered by the app. 




