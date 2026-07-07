package ionic.muses

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(WebDavPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
