package com.example.barcodescanner

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORE_REQUEST_CODE = 101
        private const val TAG = "MAIN_TAG"
    }
    private lateinit var cameraPermission: Array<String>
    private lateinit var storePermission: Array<String>
    private var imageUri: Uri? =null
    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraPermission = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        barcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)
        //check permission and take image
        binding.cameraBtn.setOnClickListener{
            if(checkCameraPermission())pickImageCamera()
            else requestCameraPermission()
        }

            //check permission and get image from galley
            binding.galleryBtn.setOnClickListener {
                if(checkStoragePermission())pickImageGallery()
                else requestStoragePermission()
            }

            //scan
            binding.scanBtn.setOnClickListener {
                if(imageUri==null){
                    showToast("pick image first")
                }else{
                    detectResultFromImage()
                }
            }

    }

    private fun detectResultFromImage() {
        try {
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            val barcodeResult = barcodeScanner!!.process(inputImage)
                .addOnSuccessListener {barcodes->
                    extractBarcodeQrCodeInfo(barcodes)
                }
                .addOnFailureListener {
                    Log.e(TAG,"detectResultFromImage:", it)
                    showToast("Scaninng failed cause of ${it.message}")
                }
        }catch (e:Exception){
            Log.e(TAG,"detectResultFromImage:", e)
        }
    }

    private fun extractBarcodeQrCodeInfo(barcodes: List<Barcode>) {
        for(barcode in barcodes){
            val bound = barcode.boundingBox
            val corners = barcode.cornerPoints
            val rawValue = barcode.rawValue
            Log.d(TAG,"extractBarcodeQrCodeInfo: rawValue: $rawValue")

            val valueType = barcode.valueType
            when(valueType){
                //write barcode type similarly
                Barcode.TYPE_WIFI->{
                    val typeWifi = barcode.wifi
                    val ssid = "${typeWifi?.ssid}"
                    val password = "${typeWifi?.password}"
                    var encryptionType = "${typeWifi?.encryptionType}"
                    if(encryptionType=="1"){
                        encryptionType="OPEN"
                    }
                    else if (encryptionType == "2"){
                        encryptionType = "WPA"
                    }else if(encryptionType=="3"){
                        encryptionType = "WEP"
                    }
                    Log.d(TAG,"extractedBarcodeQrCodeInfo: TYPE_WIFI")
                    Log.d(TAG,"extractedBarcodeQrCodeInfo: ssid: $ssid")
                    Log.d(TAG,"extractedBarcodeQrCodeInfo: password: $password")
                    Log.d(TAG,"extractedBarcodeQrCodeInfo: encryptionType: $encryptionType")
                    binding.resultTV.text= "TYPE_WIFI \n ssid: $ssid \n password: $password \n encryptionTpye: $encryptionType\n\n"
                }
                else->{
                    binding.resultTV.text= "rawValue: $rawValue"

                }
            }
        }
    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data?.data
            Log.d(TAG,"from galleryActivityResultLauncher imagerUri: $imageUri")
            binding.imageIV.setImageURI(imageUri)
        }else{
            showToast("Canceled...!")
        }
    }
    private fun pickImageCamera(){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE,"Sample Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Sample Image DESCRIPTION")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result->
        if (result.resultCode == Activity.RESULT_OK){
            val data = result.data
            Log.d(TAG,"from cameraActivityResultLaouncher imageUri: $imageUri")
            binding.imageIV.setImageURI(imageUri)
        }
    }
    private fun checkStoragePermission():Boolean{
        val result=(ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
        return result;
    }
    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storePermission,STORE_REQUEST_CODE)
    }
    private fun checkCameraPermission():Boolean{
        val result=(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) && checkStoragePermission()
        return result;
    }
    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                if(grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED
                    val storeAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED
                    if(cameraAccepted && storeAccepted){
                        pickImageCamera()
                    }else{
                        showToast("Permissions Required")
                    }
                }
            }
            STORE_REQUEST_CODE->{
                val storeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if(storeAccepted){
                    pickImageGallery()
                }else{
                    showToast("Storage Permission isrequired")
                }
            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
}