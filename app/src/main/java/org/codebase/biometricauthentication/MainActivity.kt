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
import dev.skomlach.biometric.compat.BiometricPromptCompat
import org.codebase.biometricauthentication.databinding.ActivityMainBinding
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
        val builder = BiometricPromptCompat.Builder(this).setTitle("Biometric demo")
            .setNegativeButtonText("Cancel")
         builder.build()

        binding.loginButtonId.setOnClickListener {
//            checkDeviceHasBiometric()
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

}