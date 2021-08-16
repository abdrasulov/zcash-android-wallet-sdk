package cash.z.ecc.android.uniffikation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import cash.z.ecc.client.sqlite.Network
import cash.z.ecc.client.sqlite.deriveExtfvk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    override fun onResume() {
        super.onResume()

        val viewingKey = deriveExtfvk(Network.TEST_NETWORK, TEST_EXTSK)
        findViewById<TextView>(R.id.text_info).text = viewingKey
    }

    companion object {
        const val TEST_EXTSK = "secret-extended-key-test1qd32v4k4qqqqpqps6c09xrk35mep92r2u5vkfeq5w3wfn32q7l7g2hepqkkexkvv7hx3nntqz5nk5zuclqtdct6m90g20nr75m5q8983dq32lkkuah3qwl6zv3at9sm5d0k8myntulpk09y94j6sdmj0k2spj3v28zrdh9str993x9u6yjwne2k9s76h7zagjg6fwcwanaw2tw9ffwvj407v43jk676k39f2eslvypmj699upmd5x88n2xf83szr2wzzd0x7dadnn5c2vle3z"
        const val TEST_EXTFVK = "zxviewtestsapling1qd32v4k4qqqqpqps6c09xrk35mep92r2u5vkfeq5w3wfn32q7l7g2hepqkkexkvv7hjxh8gc7nrgmy28yfktsn5lkefkeytvxcadk9pnmhuzmss8q42r9uv2j5wpm3xjnxkcjyz92mxylpfqm3tlme4sdkcgldu56xr8phd8r993x9u6yjwne2k9s76h7zagjg6fwcwanaw2tw9ffwvj407v43jk676k39f2eslvypmj699upmd5x88n2xf83szr2wzzd0x7dadnn5cce4xmm"
    }
}