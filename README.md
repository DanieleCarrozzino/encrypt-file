# EncryptFileClass
__EncryptFileClass__ is an Android Kotlin class that simplifies the process of __encrypting__ and __decrypting__ files with advanced features and customization options. 
It leverages the Android Keystore system and biometric authentication to enhance __security__. 
This class is designed to be versatile and easy to integrate into your Android applications for securing sensitive files.

## Features
- File encryption and decryption with optional biometric authentication.
- Utilizes the Android Keystore system for secure key management.
- Supports advanced customization options to tailor the behavior to your specific requirements.
- Integration with biometric authentication to protect files with fingerprint or face recognition (on supported devices).
- Handles the creation of an encrypted version of the file, without overwriting the original.

## Usage
1 __Initialization:__ Create an instance of __EncryptFileClass__ by providing a Context and the File you want to encrypt or decrypt.
2 __Customization:__ You can customize various aspects of the encryption process, such as enabling or disabling the use of biometric authentication, specifying the path to save the encrypted file, or setting a custom master key.
3 __Encryption:__ Use the __encrypt()__ method to encrypt the file. If you choose to use biometric authentication, it will prompt the user to unlock the file.
4 __Decryption:__ Use the __decrypt()__ method to decrypt the file. Again, biometric authentication can be used for added security.
5 __Callbacks:__ You can set callback functions to handle the completion and failure events during encryption and decryption.
6 __Advanced Options:__ The class provides advanced options such as deleting the original file after encryption, specifying a custom file name, and more.