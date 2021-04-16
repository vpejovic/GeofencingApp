package si.uni_lj.fri.lrk.geofencingapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    companion object {
        const val TAG = "GeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            GeofenceJobIntentService.enqueueWork(context, intent)
        }

    }
}