package com.devlomi.fireapp.activities.main.status

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.cjt2325.cameralibrary.ResultCodes
import com.devlomi.circularstatusview.CircularStatusView
import com.devlomi.fireapp.api.Constants
import com.devlomi.fireapp.api.Constants.BASE_URL_VIDEO
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.CameraActivity
import com.devlomi.fireapp.activities.MyStatusActivity
import com.devlomi.fireapp.activities.TextStatusActivity
import com.devlomi.fireapp.activities.ViewStatusActivity
import com.devlomi.fireapp.activities.main.MainActivity.Companion.CAMERA_REQUEST
import com.devlomi.fireapp.activities.main.MainActivity.Companion.REQUEST_CODE_TEXT_STATUS
import com.devlomi.fireapp.activities.main.MainViewModel
import com.devlomi.fireapp.activities.main.chats.ChatsAdapter
import com.devlomi.fireapp.activities.main.status.StatusFragmentEvent.OnActivityResultEvent
import com.devlomi.fireapp.activities.main.status.StatusFragmentEvent.StatusInsertedEvent
import com.devlomi.fireapp.adapters.StatusAdapter
import com.devlomi.fireapp.fragments.BaseFragment
import com.devlomi.fireapp.interfaces.StatusFragmentCallbacks
import com.devlomi.fireapp.model.Ads.Ads.AdsList
import com.devlomi.fireapp.model.constants.MessageType
import com.devlomi.fireapp.model.constants.StatusType
import com.devlomi.fireapp.model.realms.TextStatus
import com.devlomi.fireapp.model.realms.UserStatuses
import com.devlomi.fireapp.utils.*
import com.devlomi.fireapp.utils.mediastore.MediaStoreFileInfo
import com.devlomi.fireapp.utils.mediastore.MediaStoreUtil
import com.devlomi.fireapp.utils.network.FireManager
import com.devlomi.fireapp.utils.network.StatusManager
import com.devlomi.fireapp.views.HeaderViewDecoration
import com.devlomi.fireapp.views.TextViewWithShapeBackground
import com.droidninja.imageeditengine.ImageEditor
import com.google.android.gms.ads.AdView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.zhihu.matisse.Matisse
import io.reactivex.rxkotlin.addTo
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_status.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import com.devlomi.fireapp.views.videoView.ui.widget.VideoView


class StatusFragment : BaseFragment(), StatusAdapter.OnClickListener {
    private lateinit var adapter: StatusAdapter
    var statusesList: RealmResults<UserStatuses>? = null
    private var myStatuses: UserStatuses? = null
    private var decor: HeaderViewDecoration? = null
    private var header1pos = 0
    private var header2pos = 0
    private var header1Title: String? = null
    private var header2Title = ""
    override var adView: AdView? = null
    private var callbacks: StatusFragmentCallbacks? = null
    private val statusManager = StatusManager()

    private var MAX_STATUS_VIDEO_TIME = 0

    private lateinit var btnViewMyStatuses: ImageButton
    private lateinit var tvLastStatusTime: TextView
    private lateinit var tvTextStatus: TextViewWithShapeBackground
    private lateinit var circularStatusView: CircularStatusView
    private lateinit var profileImage: ImageView
    private lateinit var rowStatusContainer: ConstraintLayout
    private lateinit var textStatusFab: ImageView
    private lateinit var fab: FloatingActionButton
    private lateinit var _ads_: View
    private lateinit var ic_option_: ImageView
    private lateinit var image_adv: ImageView
    private lateinit var videoView: VideoView

    private lateinit var cld: ConnectionLiveData

    private val TAG = "StatusFragment"

    private val viewModel: MainViewModel by activityViewModels()
    override fun showAds(): Boolean {
        return resources.getBoolean(R.bool.is_status_ad_enabled)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as StatusFragmentCallbacks

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //since we are using <include> the app sometimes crashes, to solve that we are instantiate it using findViewById
        btnViewMyStatuses = view.findViewById(R.id.btn_view_my_statuses)
        circularStatusView = view.findViewById(R.id.circular_status_view)
        tvLastStatusTime = view.findViewById(R.id.tv_last_status_time)
        tvTextStatus = view.findViewById(R.id.tv_text_status)
        rowStatusContainer = view.findViewById(R.id.row_status_container)
        profileImage = view.findViewById(R.id.profile_image)
        textStatusFab = view.findViewById(R.id.text_status_fab)
        fab = view.findViewById(R.id.open_new_chat_fab)
        _ads_ = view.findViewById(R.id._ads_)
        videoView = _ads_.findViewById(R.id.video_adv)
        ic_option_ = _ads_.findViewById(R.id.ic_option_)
        image_adv = _ads_.findViewById(R.id.image_adv)
        adView = ad_view
        adViewInitialized(adView)

        checkNetworkConnection()

        MAX_STATUS_VIDEO_TIME = resources.getInteger(R.integer.max_status_video_time)
        btnViewMyStatuses.setOnClickListener(View.OnClickListener {
            if (myStatuses == null) return@OnClickListener
            startActivity(Intent(activity, MyStatusActivity::class.java))
        })
        statusesList = RealmHelper.getInstance().allStatuses
        initMyStatuses()
        circularStatusView.visibility = View.GONE
        initAdapter()
        rowStatusContainer.setOnClickListener {
            if (myStatuses?.filteredStatuses?.isNotEmpty() == true) {
                val intent = Intent(activity, ViewStatusActivity::class.java)
                intent.putExtra(IntentUtils.UID, myStatuses?.userId)
                startActivity(intent)
            } else {
                callbacks?.openCamera()
            }
        }

        textStatusFab.setOnClickListener {
            startActivityForResult(
                Intent(
                    requireActivity(), TextStatusActivity::class.java
                ), REQUEST_CODE_TEXT_STATUS
            )
        }

        fab.setOnClickListener {
            startCamera()
        }

        viewModel.statusLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer
            { statusFragmentEvent ->
                when (statusFragmentEvent) {
                    is StatusInsertedEvent -> statusInserted()
                    is OnActivityResultEvent -> {
                        val requestCode = statusFragmentEvent.requestCode
                        val resultCode = statusFragmentEvent.resultCode
                        val data = statusFragmentEvent.data

                        if (requestCode == CAMERA_REQUEST) {
                            onCameraActivityResult(resultCode, data)


                        } else if (requestCode == ImageEditor.RC_IMAGE_EDITOR && resultCode == Activity.RESULT_OK) {
                            data?.getStringExtra(ImageEditor.EXTRA_EDITED_PATH)?.let { imagePath ->
                                onImageEditSuccess(imagePath)
                            }

                        } else if (requestCode == REQUEST_CODE_TEXT_STATUS && resultCode == Activity.RESULT_OK) {
                            data.getParcelableExtra<TextStatus>(IntentUtils.EXTRA_TEXT_STATUS)
                                ?.let { textStatus ->
                                    onTextStatusResult(textStatus)
                                }

                        }
                    }
                }
            })

        viewModel.queryTextChange.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer
            { newText ->
                onQueryTextChange(newText)
            })

    }

    private fun startCamera() {
        Dexter.withContext(requireActivity())
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val intent = Intent(requireActivity(), CameraActivity::class.java)
                    intent.putExtra(IntentUtils.CAMERA_VIEW_SHOW_PICK_IMAGE_BUTTON, true)
                    intent.putExtra(IntentUtils.IS_STATUS, true)
                    startActivityForResult(intent, CAMERA_REQUEST)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    Toast.makeText(
                        requireActivity(),
                        R.string.missing_permissions,
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }).check()


    }

    private fun initMyStatuses() {
        myStatuses = RealmHelper.getInstance().getUserStatuses(FireManager.uid)
    }

    fun setMyStatus() {
        if (myStatuses == null) initMyStatuses()
        if (myStatuses != null && myStatuses?.filteredStatuses?.isNotEmpty() == true) {
            val lastStatus = myStatuses?.statuses?.last()
            val statusTime = TimeHelper.getStatusTime(lastStatus?.timestamp ?: Date().time)
            tvLastStatusTime.text = statusTime
            btnViewMyStatuses.visibility = View.VISIBLE
            circularStatusView.visibility = View.VISIBLE
            if (lastStatus?.type == StatusType.IMAGE || lastStatus?.type == StatusType.VIDEO) {
                tvTextStatus.visibility = View.GONE
                profileImage.visibility = View.VISIBLE
                Glide.with(requireActivity()).load(lastStatus.thumbImg).into(profileImage)
            } else if (lastStatus?.type == StatusType.TEXT) {
                tvTextStatus.visibility = View.VISIBLE
                profileImage.visibility = View.GONE
                val textStatus = lastStatus.textStatus
                tvTextStatus.text = textStatus.text
                tvTextStatus.setShapeColor(
                    Color.parseColor(
                        textStatus?.backgroundColor ?: "#000000"
                    )
                )
            }
        } else {
            circularStatusView.visibility = View.GONE
            tvTextStatus.visibility = View.GONE
            profileImage.visibility = View.VISIBLE
            Glide.with(requireActivity()).load(SharedPreferencesManager.getThumbImg())
                .into(profileImage)
            btnViewMyStatuses.visibility = View.GONE
            tvLastStatusTime.text = getString(R.string.tap_to_add_status)
        }
    }


    fun onCameraActivityResult(resultCode: Int, data: Intent) {
        if (resultCode != ResultCodes.CAMERA_ERROR_STATE) {
            if (resultCode == ResultCodes.IMAGE_CAPTURE_SUCCESS) {
                val path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT)
                ImageEditorRequest.open(activity, path)
            } else if (resultCode == ResultCodes.VIDEO_RECORD_SUCCESS) {
                data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT)?.let { path ->
                    uploadVideoStatus(path)
                }
            } else if (resultCode == ResultCodes.PICK_IMAGE_FROM_CAMERA) {
                if (BuildVerUtil.isApi29OrAbove()) {
                    handleGalleryRequestApi29(data)
                } else {
                    val mPaths = Matisse.obtainPathResult(data)
                    for (mPath in mPaths) {
                        if (!FileUtils.isFileExists(mPath)) {
                            Toast.makeText(
                                activity,
                                MyApp.context().resources.getString(R.string.image_video_not_found),
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }


                    //Check if it's a video
                    if (FileUtils.isPickedVideo(mPaths[0])) {

                        //check if video is longer than 30sec
                        val mediaLengthInMillis = Util.getMediaLengthInMillis(context, mPaths[0])
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(mediaLengthInMillis)
                        if (seconds <= MAX_STATUS_VIDEO_TIME) {
                            for (mPath in mPaths) {
                                uploadVideoStatus(mPath)
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                MyApp.context().resources.getString(R.string.video_length_is_too_long),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        //if it's only one image open image editor
                        if (mPaths.size == 1) ImageEditorRequest.open(
                            activity, mPaths[0]
                        ) else for (path in mPaths) {
                            uploadImageStatus(path)
                        }
                    }
                }
            }
        }
    }

    private fun handleGalleryRequestApi29(data: Intent) {
        try {


            val uris = Matisse.obtainResult(data)
            if (uris.isEmpty()) return


            if (MediaStoreFileInfo.isImageType(requireActivity(), uris[0])!!) {
                //if it's only one image open image editor
                if (uris.size == 1) {
                    val file = File(requireActivity().cacheDir, uris[0].lastPathSegment)
                    MediaStoreUtil.saveUriToFile(uris[0], file)
                    ImageEditorRequest.open(
                        activity, file.path
                    )

                } else for (uri in uris) {
                    val file = File(requireActivity().cacheDir, uri.lastPathSegment)
                    MediaStoreUtil.saveUriToFile(uris[0], file)
                    uploadImageStatus(file.path)
                }
            } else {
                for (uri in uris) {
                    val duration = RealPathUtil.getAudioDuration(requireActivity(), uri)
                    if (duration != -1) {
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration.toLong())

                        if (seconds <= MAX_STATUS_VIDEO_TIME) {
                            val file = File(requireActivity().cacheDir, uri.lastPathSegment)

                            MediaStoreUtil.saveUriToFile(uri, file)

                            uploadVideoStatus(file.path)
                        } else {
                            Toast.makeText(
                                activity,
                                MyApp.context().resources.getString(R.string.video_length_is_too_long),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireActivity(), R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initAdapter() {
        adapter = StatusAdapter(statusesList, true, context, this@StatusFragment)
//        rv_status.layoutManager = LinearLayoutManager(context)
        rv_status.adapter = adapter
        decor = HeaderViewDecoration(context)
        decor?.let {
            rv_status.addItemDecoration(it)
        }

    }

    private fun setupHeaders() {
        header1pos = -1
        header2pos = -1
        statusesList?.let {


            for (userStatuses in it) {
                if (!userStatuses.isAreAllSeen) {
                    if (header1pos == -1) {
                        header1pos = it.indexOf(userStatuses)
                    }
                } else {
                    if (header2pos == -1) {
                        header2pos = it.indexOf(userStatuses)
                        break
                    }
                }
            }
        }
        //if the statuses are all seen,then set the header title as Viewed updates
        if (header1pos == -1) {
            header1Title = MyApp.context().resources.getString(R.string.viewed_statuses)
            header2Title = MyApp.context().resources.getString(R.string.viewed_statuses)
        } else {
            header1Title = MyApp.context().resources.getString(R.string.recent_updates)
            header2Title = MyApp.context().resources.getString(R.string.viewed_statuses)
        }
    }

    private fun uploadVideoStatus(path: String) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(
                activity,
                MyApp.context().resources.getString(R.string.no_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Toast.makeText(activity, R.string.uploading_status, Toast.LENGTH_SHORT).show()
        disposables.add(statusManager.uploadStatus(path, StatusType.VIDEO, true)
            .subscribe { status, throwable ->
                if (throwable != null) {
                    Toast.makeText(
                        activity,
                        MyApp.context().resources.getString(R.string.error_uploading_status),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    setMyStatus()
                    Toast.makeText(
                        activity,
                        MyApp.context().resources.getString(R.string.status_uploaded),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })


    }

    private fun uploadImageStatus(path: String) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(
                MyApp.context(),
                MyApp.context().resources.getString(R.string.no_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Toast.makeText(
            MyApp.context(),
            MyApp.context().resources.getString(R.string.uploading_status),
            Toast.LENGTH_SHORT
        ).show()
        val mPath = compressImage(path)
        statusManager.uploadStatus(mPath, StatusType.IMAGE, false).subscribe { status, throwable ->
            if (throwable != null) {
                Toast.makeText(
                    activity,
                    MyApp.context().resources.getString(R.string.error_uploading_status),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                setMyStatus()
                Toast.makeText(
                    activity,
                    MyApp.context().resources.getString(R.string.status_uploaded),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addTo(disposables)
    }

    override fun onResume() {
        super.onResume()
        updateHeaders()
        setMyStatus()
        //fetch status when user swipes to this page
        callbacks?.fetchStatuses()
    }

    private fun updateHeaders() {
        if (decor != null) {
            setupHeaders()
            decor?.updateHeaders(header1pos, header2pos, header1Title, header2Title)
            adapter.notifyDataSetChanged()
        }
    }

    fun statusInserted() {
        try {
            //Fix for crash 'fragment not attached to context'
            updateHeaders()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onQueryTextChange(newText: String?) {
        super.onQueryTextChange(newText)
        if (adapter != null) {

            adapter.filter(newText)
        }
    }

    override fun onSearchClose() {
        super.onSearchClose()
        adapter = StatusAdapter(statusesList, true, activity, this@StatusFragment)

        rv_status?.let {
            it.adapter = adapter
        }

    }

    override fun onStatusClick(view: View, userStatuses: UserStatuses) {
        val intent = Intent(activity, ViewStatusActivity::class.java)
        intent.putExtra(IntentUtils.UID, userStatuses.userId)
        startActivity(intent)
    }

    //compress image when user chooses an image from gallery
    private fun compressImage(imagePath: String): String {
        //generate file in sent images folder
        val file = DirManager.generateFile(MessageType.SENT_IMAGE)
        //compress image and copy it to the given file
        BitmapUtils.compressImage(imagePath, file)
        return file.path
    }

    fun onImageEditSuccess(imagePath: String) {
        uploadImageStatus(imagePath)
    }

    fun onTextStatusResult(textStatus: TextStatus) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(MyApp.context(), R.string.no_internet_connection, Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(MyApp.context(), R.string.uploading_status, Toast.LENGTH_SHORT).show()
            statusManager.uploadTextStatus(textStatus).subscribe({
                setMyStatus()
            }, { throwable ->

            }).addTo(disposables)

        }
    }

    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(requireActivity().application)

        cld.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                RetrofitInstance.api.getAds(token = Constants.TOKEN).clone()
                    .enqueue(object : Callback<AdsList> {
                        override fun onResponse(call: Call<AdsList>, response: Response<AdsList>) {

                            if (response.body()?.data != null) {
                                try {
                                    if (!response.body()?.data!![0].media.contains(".mp4")) {
                                        _ads_.visibility = View.VISIBLE
                                        videoView.visibility = View.GONE
                                        image_adv.visibility = View.VISIBLE
                                        Glide.with(MyApp.context())
                                            .load(BASE_URL_VIDEO + response.body()?.data!![0].media)
                                            .into(image_adv)
                                    }
                                } catch (e: NullPointerException) {
                                    Log.e(TAG, "onBindViewHolder: " + e.message)
                                    _ads_.visibility = View.GONE
                                }
                            } else {
                                _ads_.visibility = View.GONE
                            }


                        }

                        override fun onFailure(call: Call<AdsList>, t: Throwable) {

                        }

                    })
            } else {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.no_internt),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }


}