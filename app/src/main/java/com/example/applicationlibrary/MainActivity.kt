package com.example.applicationlibrary

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.encrypt.encryptfile.EncryptFileClass
import com.example.applicatoinlibrary.R
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var filePathEncrypted = EncryptFileClass(this, File("your\\file\\path")).encrypt()
    }
}