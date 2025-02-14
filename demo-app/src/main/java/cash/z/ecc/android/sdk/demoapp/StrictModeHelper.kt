package cash.z.ecc.android.sdk.demoapp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import cash.z.ecc.android.sdk.demoapp.util.AndroidApiVersion

object StrictModeHelper {

    fun enableStrictMode() {
        configureStrictMode()

        // Workaround for Android bug
        // https://issuetracker.google.com/issues/36951662
        // Not needed if target O_MR1 and running on O_MR1
        // Don't really need to check target, because of Google Play enforcement on targetSdkVersion for app updates
        if (!AndroidApiVersion.isAtLeastO_MR1) {
            Handler(Looper.getMainLooper()).postAtFrontOfQueue { configureStrictMode() }
        }
    }

    @SuppressLint("NewApi")
    private fun configureStrictMode() {
        StrictMode.enableDefaults()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().apply {
                detectAll()
                penaltyLog()
            }.build()
        )

        // Don't enable missing network tags, because those are noisy.
        if (AndroidApiVersion.isAtLeastO) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectActivityLeaks()
                    detectCleartextNetwork()
                    detectContentUriWithoutPermission()
                    detectFileUriExposure()
                    detectLeakedClosableObjects()
                    detectLeakedRegistrationObjects()
                    detectLeakedSqlLiteObjects()
                    if (AndroidApiVersion.isAtLeastP) {
                        // Disable because this is mostly flagging Android X and Play Services
                        // builder.detectNonSdkApiUsage();
                    }
                }.build()
            )
        } else {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectAll()
                    penaltyLog()
                }.build()
            )
        }
    }
}
