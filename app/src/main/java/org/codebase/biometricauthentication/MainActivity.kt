package org.codebase.biometricauthentication

import android.app.Activity
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dev.skomlach.biometric.compat.AuthenticationFailureReason
import dev.skomlach.biometric.compat.AuthenticationResult
import dev.skomlach.biometric.compat.BiometricApi
import dev.skomlach.biometric.compat.BiometricAuthRequest
import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricCryptographyPurpose
import dev.skomlach.biometric.compat.BiometricManagerCompat
import dev.skomlach.biometric.compat.BiometricPromptCompat
import dev.skomlach.biometric.compat.BiometricType
import dev.skomlach.biometric.compat.crypto.CryptographyManager
import org.codebase.biometricauthentication.databinding.ActivityMainBinding
import java.nio.charset.Charset
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButtonId.setOnClickListener {
//            checkDeviceHasBiometric()
            startBioAuth()
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object :
            BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Authentication error: $errString",
                    Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(this@MainActivity, "Authentication succeeded!",
                    Toast.LENGTH_SHORT).show()

            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Authentication failed",
                    Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate with Fingerprint")
            .setNegativeButtonText("Use account password")
            .setConfirmationRequired(true)
//            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        binding.imageFingerPrintId.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(this)
        Log.e("biometric", "App using biometric.")

        when(biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.e("biometric_success", "App can authenticate using biometric.")
                binding.tvMessageId.text = "App can authenticate using biometric."
                binding.loginButtonId.isEnabled = true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e("biometric error", "Biometric feature currently unavailable")
                binding.tvMessageId.text = "Biometric feature currently unavailable"
                binding.loginButtonId.isEnabled = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e("not enrolled", "biometric not enrolled")
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or
                    DEVICE_CREDENTIAL)
                }
                binding.loginButtonId.isEnabled = false

                activityResultLauncher.launch(enrollIntent)
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e("biometric error", "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
                binding.tvMessageId.text = "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.e("biometric error", "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
                binding.tvMessageId.text = "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"

            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.e("biometric error", BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED.toString())
                binding.tvMessageId.text = "BIOMETRIC_ERROR_UNSUPPORTED"
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.e("biometric error", "BIOMETRIC_STATUS_UNKNOWN")
                binding.tvMessageId.text = "BIOMETRIC_STATUS_UNKNOWN"

            }
        }
    }

    //
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.e("result ok", "activity launching")
        }
    }

    private fun startBioAuth() {
        val iris = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_IRIS,
            BiometricConfirmation.ANY
        )
        val faceId = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_FACE,
            BiometricConfirmation.ANY
        )
        val fingerprint = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_FINGERPRINT,
            BiometricConfirmation.ANY
        )
        var title = ""
        val currentBiometric =
            if (BiometricManagerCompat.isHardwareDetected(iris)
                && BiometricManagerCompat.hasEnrolled(iris)
            ) {
                title =
                    "Your eyes are not only beautiful, but you can use them to unlock our app"
                iris
            } else
                if (BiometricManagerCompat.isHardwareDetected(faceId)
                    && BiometricManagerCompat.hasEnrolled(faceId)
                ) {
                    title = "Use your smiling face to enter the app"
                    faceId
                } else if (BiometricManagerCompat.isHardwareDetected(fingerprint)
                    && BiometricManagerCompat.hasEnrolled(fingerprint)
                ) {
                    title = "Your unique fingerprints can unlock this app"
                    fingerprint
                } else {
                    null
                }

        currentBiometric?.let { biometricAuthRequest ->
            if (BiometricManagerCompat.isBiometricSensorPermanentlyLocked(biometricAuthRequest)
                || BiometricManagerCompat.isLockOut(biometricAuthRequest)
            ) {
                showToast("Biometric not available right now. Try again later")
                return
            }

            val prompt = BiometricPromptCompat.Builder(this).apply {
                this.setTitle(title)
//                this.setNegativeButton("Cancel", null)
                this.setEnabledNotification(false)//hide notification
                this.setEnabledBackgroundBiometricIcons(false)//hide duplicate biometric icons above dialog
                this.setCryptographyPurpose(BiometricCryptographyPurpose(BiometricCryptographyPurpose.ENCRYPT))//request Cipher for encryption
            }
            if(!prompt.isSilentAuthEnabled()){
                showToast("Unable to use Silent Auth on current device :|")
                return
            }
            prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
                override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                    super.onSucceeded(confirmed)
                    val encryptedData = CryptographyManager.encryptData(
                        "Hello, my friends".toByteArray(Charset.forName("UTF-8")),
                        confirmed
                    )

                    showToast("User authorized :)\n Biometric used for Encryption=${encryptedData!!.biometricType}\n EncryptedData=${encryptedData.data}; InitializationVector=${encryptedData.initializationVector};")
                }

                override fun onCanceled() {
                    showToast("Auth canceled :|")
                }

                override fun onFailed(
                    reason: AuthenticationFailureReason?,
                    dialogDescription: CharSequence?
                ) {
                    showToast("Fatal error happens :(\nReason $reason")
                }

                override fun onUIOpened() {}

                override fun onUIClosed() {}
            })
        } ?: run {
            showToast("No available biometric on this device")
        }

    }

    private fun showToast(s: String) {
        Toast.makeText(this@MainActivity, s, Toast.LENGTH_SHORT).show()
    }

}