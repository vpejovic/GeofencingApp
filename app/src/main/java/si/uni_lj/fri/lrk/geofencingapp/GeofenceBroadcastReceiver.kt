package si.uni_lj.fri.lrk.geofencingapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    companion object {
        const val TAG = "GeofenceBR"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive")
        if (context != null && intent != null) {

            // TODO: Enqueue a JobIntentService
            Log.d(TAG, "enqueuing work")
            GeofenceJobIntentService.enqueueWork(context, intent)
        }

    }
}