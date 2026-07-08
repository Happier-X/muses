package com.muses.player

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(AudioPlayerPlugin::class.java)
        registerPlugin(WebDavPlugin::class.java)
        registerPlugin(LocalLibraryPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
