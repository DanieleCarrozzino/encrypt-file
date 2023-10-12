package com.encrypt.encryptfile

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.EncryptedFile

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import java.io.PrintWriter

class EncryptFileClass constructor(
    private val context: Context,
    private val file: File
){

    /**
     * State result after an attempt
     * of decryption or encryption
     * */
    enum class EncryptState {
        ENCRYPT,
        DECRYPT,
        ERROR
    }

    /**
     * Build KeyGenParameterSpec to create the master key
     * */
    private val advancedSpec = KeyGenParameterSpec.Builder(
        "key_gen_master_key_v_1",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        setKeySize(256)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setUserAuthenticationParameters(60, KeyProperties.AUTH_BIOMETRIC_STRONG) // must be larger than 0
            setUserAuthenticationRequired(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setUnlockedDeviceRequired(true)
            setIsStrongBoxBacked(true)
        }
    }.build()

    /**
     * Get or create the master key,
     * it's a string used to encrypt
     * and decrypt files
     * */
    private var masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    /**
     * New file name encrypted
     * */
    private var fileNameEncrypted = "Encrypted_${file.name}"

    /**
     * New path encrypted file
     * */
    private var fullPathEncrypted = "${file.parent}/$fileNameEncrypted"

    /**
     * File to encrypt
     * */
    private val encryptedFile = EncryptedFile.Builder(
        File(fullPathEncrypted),
        context,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)

    /**
     * Biometric Callback to unlock the usages of this class
     * */
    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(result)
            // Unlocked - valid key
            // I can encrypt/decrypt the file
            if(encrypt) encrypt() else decrypt()
        }
        override fun onAuthenticationError(
            errorCode: Int, errString: CharSequence
        ) {
            super.onAuthenticationError(errorCode, errString)
            // Handle error.
            when(errorCode){
                ERROR_NO_BIOMETRICS -> {
                    failure("There isn\\'t any biometrics enrolled")
                }
            }
        }
    }

    /**
     * Init master key on different usages
     * */
    private fun initMasterKey(){
        //set master key
        masterKey =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !customMasterKey && useBiometric)
                MasterKeys.getOrCreate(advancedSpec)
            else masterKey
    }

    /**
     * Create result path
     * */
    private fun createPath(){
        if(pathToSave.isNotEmpty() && fileName.isNotEmpty()){
            fullPathEncrypted = "$pathToSave/$fileName"
        }
        else if(pathToSave.isNotEmpty()){
            fullPathEncrypted = "$pathToSave/$fileNameEncrypted"
        }
        else if(fileName.isNotEmpty()){
            fullPathEncrypted = "${file.parent}/$fileName"
        }
    }

    /**
     * Encrypt core
     * */
    private fun encryptFile(){
        // If the file already exists
        // I don't want to delete or
        // overwrite the other file
        // with the same name
        if(File(fullPathEncrypted).exists())
            throw Exception("The encrypted file already exists!")

        // Copy file
        encryptedFile.build().openFileOutput().use { outputStream ->
            file.inputStream().use {
                outputStream.write(it.readBytes())
            }
        }

        // Delete the not encrypted file
        if(deleteFile)
            deleteFile(file)
        else if(emptyFile){
            with(PrintWriter(file)){
                print("")
                close()
            }
        }

        // Call callback
        completed(EncryptState.ENCRYPT)
    }

    /**
     * Decrypt core
     * */
    private fun decryptFile(){
        // If the file doesn't exists
        // I have to create it
        createFile(file)

        // Copy file
        encryptedFile.build().openFileInput().use { inputStream ->
            file.outputStream().use {
                it.write(inputStream.readBytes())
            }
        }

        // Delete the encrypted file
        deleteFile(File(fullPathEncrypted))

        // Call callback
        completed(EncryptState.DECRYPT)
    }

    private fun deleteFile(file: File){
        if(file.exists())
            file.delete()
    }

    private fun createFile(path : String){
        if(!File(path).exists())
            File(path).createNewFile()
    }

    private fun createFile(file : File){
        if(!file.exists())
            file.createNewFile()
    }

    init {
        val dir = File("${file.parent}/Encrypt")
        if(!dir.exists()) dir.mkdir()
    }

    private var encrypt : Boolean = false
    private fun biometricPromptRequest(fragment: Fragment, encrypt:Boolean){

        // It's used to create the master key
        useBiometric = true

        this.encrypt = encrypt

        if(encrypt && !file.exists()) {
            throw java.lang.IllegalArgumentException("File doesn't exists")
        }

        val biometricPrompt = BiometricPrompt(
            fragment,
            ContextCompat.getMainExecutor(context),
            authenticationCallback
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if(encrypt) "Lock?" else "Unlock?")
            .setDescription(
                if(encrypt) "Would you like to lock this file?"
                else        "Would you like to unlock this file?")
            .setAllowedAuthenticators(if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG else BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        fun getDefaultPathEncryptedFromFile(file : File) : String{
            return "${file.parent}/Encrypted_${file.name}"
        }
    }

    /*
    * CUSTOMIZATION
    * */

    private var useBiometric    : Boolean   = false
    private var deleteFile      : Boolean   = false
    private var emptyFile       : Boolean   = false
    private var customMasterKey : Boolean   = false;
    private var pathToSave      : String    = ""
    private var fileName        : String    = ""
    private lateinit var completed : (EncryptState) -> Unit
    private lateinit var failure : (String) -> Unit

    fun deleteOldFile(value : Boolean) : EncryptFileClass{
        deleteFile = value
        return this
    }

    fun emptyOldFile(value : Boolean) : EncryptFileClass{
        emptyFile = value
        return this
    }

    fun setResultPath(path : String) : EncryptFileClass{
        pathToSave = path
        return this
    }

    fun setResultPath(path : String, fileName : String) : EncryptFileClass{
        fullPathEncrypted = "$path/$fileName"
        return this
    }

    fun setFileName(name : String) : EncryptFileClass{
        fileName = name
        return this
    }

    fun setMasterKey(key : String) : EncryptFileClass {
        customMasterKey = true;
        masterKey = key
        return this
    }

    fun setCompletedCallback(completed: (EncryptState) -> Unit) : EncryptFileClass{
        this.completed = completed
        return this
    }

    fun setFailureCallback(failure: (String) -> Unit) : EncryptFileClass{
        this.failure = failure
        return this
    }

    /*
    * BUILD METHODS
    * */

    /**
     * Encrypt file with adding
     * the biometric check
     * */
    fun encryptWithBiometric(fragment: Fragment) : String {
        biometricPromptRequest(fragment, true)
        return fullPathEncrypted
    }

    /**
     * Decrypt file with adding
     * the biometric check
     * */
    fun decryptWithBiometric(fragment: Fragment) : String {
        biometricPromptRequest(fragment, false)
        return fullPathEncrypted
    }

    fun encrypt() : String {
        initMasterKey()
        createPath()
        encryptFile()
        return fullPathEncrypted
    }

    fun decrypt() : String {
        initMasterKey()
        createPath()
        decryptFile()
        return fullPathEncrypted
    }
}