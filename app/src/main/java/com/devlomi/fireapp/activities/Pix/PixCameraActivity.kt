package com.devlomi.fireapp.activities.Pix

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.main.options
import com.devlomi.fireapp.adapters.PixAdapter
import com.devlomi.fireapp.databinding.ActivityPixCameraBinding
import com.devlomi.fireapp.extensions.getFileName
import com.devlomi.fireapp.model.Ads.SessionManger.getToken
import com.devlomi.fireapp.utils.UploadRequestBody
import com.devlomi.fireapp.utils.fragmentBody
import io.ak1.pix.helpers.PixBus
import io.ak1.pix.helpers.PixEventCallback
import io.ak1.pix.helpers.addPixToActivity
import io.ak1.pix.helpers.showStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PixCameraActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

    private lateinit var binding: ActivityPixCameraBinding

    val listImages: MutableList<MultipartBody.Part> = mutableListOf();
    private var TOKTEN: String = ""
    private val TAG = "PixCameraActivity"

    private val resultsFragment = ResultsFragment {
        showCameraFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPixCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun showCameraFragment() {
        addPixToActivity(R.id.container_pix, options) { results ->
            when (results.status) {
                PixEventCallback.Status.SUCCESS -> {
                    showResultsFragment()
                    results.data.forEach { uri ->
                        val parcelFileDescriptor: ParcelFileDescriptor =
                            contentResolver.openFileDescriptor(
                                uri,
                                "r",
                                null
                            )!!
                        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                        val file =
                            File(
                                cacheDir,
                                contentResolver.getFileName(uri)
                            )
                        val outputStream = FileOutputStream(file)
                        inputStream.copyTo(outputStream)
                        val body = UploadRequestBody(file, "image", this@PixCameraActivity)
                        listImages.add(
                            MultipartBody.Part.createFormData(
                                "image[]",
                                "${System.currentTimeMillis()}.jpg",
                                body
                            )
                        )
                    }
                    if (listImages.isNotEmpty()) {
                        binding.save.isEnabled = true

                    }
                    resultsFragment.setList(results.data)
                }
                PixEventCallback.Status.BACK_PRESSED -> {
                    supportFragmentManager.popBackStack()
                }
            }

        }
    }

    private fun showResultsFragment() {
        showStatusBar()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_pix, resultsFragment).commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.container_pix)
        if (f is ResultsFragment)
            super.onBackPressed()
        else
            PixBus.onBackPressedEvent()
    }

    override fun onProgressUpdate(percentage: Int) {
        Log.e(TAG, "onProgressUpdate: ${percentage}")
    }

    private fun uploadImages() {
        GlobalScope.launch(Dispatchers.Main) {
            RetrofitInstance.api
        }
    }
}

class ResultsFragment(private val clickCallback: View.OnClickListener) : Fragment() {
    private val customAdapter = PixAdapter()

    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: List<Uri>) {
        customAdapter.apply {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = fragmentBody(requireActivity(), customAdapter, clickCallback)
}
