package com.generalscan.sdkapp.system.pref

/**
 * Created by Alex Li on 27/7/2016.
 */

/**
 * Created by alexli on 7/24/13.
 */
class BarcodeBroadcastPreferences : BasePreferences() {
    companion object {

        var enableBroadcast: Boolean
            get() = getBoolean("pref_barcode_broadcast_enable_broadcast")
            set(value) = putBoolean("pref_barcode_broadcast_enable_broadcast", value)

        var intentAction: String
            get() = getString("pref_barcode_broadcast_intent_action")
            set(value) = putString("pref_barcode_broadcast_intent_action", value)

        var intentStringExtra: String
            get() = getString("pref_barcode_broadcast_string_extra")
            set(value) = putString("pref_barcode_broadcast_string_extra", value)

        var intentRawExtra: String
            get() = getString("pref_barcode_broadcast_raw_extra")
            set(value) = putString("pref_barcode_broadcast_raw_extra", value)
    }

}