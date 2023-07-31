package com.devlomi.fireapp.activities.main

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.devlomi.fireapp.api.Constants
import com.devlomi.fireapp.api.Constants.BASE_URL_VIDEO
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.*
import com.devlomi.fireapp.activities.main.calls.CallsFragment
import com.devlomi.fireapp.activities.main.chats.FragmentChats
import com.devlomi.fireapp.activities.main.status.StatusFragment
import com.devlomi.fireapp.activities.settings.SettingsActivity
import com.devlomi.fireapp.adapters.CallsAdapter
import com.devlomi.fireapp.common.ViewModelFactory
import com.devlomi.fireapp.events.ExitUpdateActivityEvent
import com.devlomi.fireapp.extensions.observeValueEvent
import com.devlomi.fireapp.interfaces.FragmentCallback
import com.devlomi.fireapp.interfaces.StatusFragmentCallbacks
import com.devlomi.fireapp.job.DailyBackupJob
import com.devlomi.fireapp.job.SaveTokenJob
import com.devlomi.fireapp.job.SetLastSeenJob
import com.devlomi.fireapp.model.Ads.Ads.AdsList
import com.devlomi.fireapp.model.realms.User
import com.devlomi.fireapp.services.FCMRegistrationService
import com.devlomi.fireapp.services.InternetConnectedListener
import com.devlomi.fireapp.services.NetworkService
import com.devlomi.fireapp.utils.*
import com.devlomi.fireapp.utils.network.FireManager
import com.devlomi.fireapp.views.dialogs.IgnoreBatteryDialog
import com.devlomi.fireapp.views.sticker.StickerLoader
import com.devlomi.fireapp.views.videoView.ui.widget.VideoView
import com.droidninja.imageeditengine.ImageEditor
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.ak1.pix.models.*
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

var options = Options()

class MainActivity : BaseActivity(), FabRotationAnimation.RotateAnimationListener, FragmentCallback,
    StatusFragmentCallbacks {
    private var isInSearchMode = false

    private lateinit var toolbar: Toolbar
    private lateinit var tvSelectedChatCount: TextView
    private lateinit var searchView: SearchView

    private val TAG = "MainActivity"

    private lateinit var bottomNavigationView: BottomNavigationView


    private var users: List<User>? = null
    private var fireListener: FireListener? = null

    //    private var adapter: ViewPagerAdapter? = null
    private lateinit var rotationAnimation: FabRotationAnimation

    private var currentPage = 0

    private lateinit var viewModel: MainViewModel

    private var ignoreBatteryDialog: IgnoreBatteryDialog? = null


    override fun enablePresence(): Boolean {
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
//        checkNetworkConnection()
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(this.application)
        ).get(MainViewModel::class.java)


        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        Task().execute("")

        rotationAnimation = FabRotationAnimation(this)

        fireListener = FireListener()
        startServices()


        users = RealmHelper.getInstance().listOfUsers



        viewModel.saveAppVersion()

        if (!SharedPreferencesManager.hasAgreedToPrivacyPolicy()) {
            showPrivacyAlertDialog()
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val pkg = packageName
                val pm = getSystemService(PowerManager::class.java)
                val showDialog = resources.getBoolean(R.bool.ignore_battery_optimizations_dialog);
                if (showDialog && !pm.isIgnoringBatteryOptimizations(pkg) && !SharedPreferencesManager.isDoNotShowBatteryOptimizationAgain()) {
                    showBatteryOptimizationDialog()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModel.deleteOldMessagesIfNeeded()
        viewModel.checkForUpdate().subscribe({ needsUpdate ->
            if (needsUpdate) {
                startUpdateActivity()
            } else {
                EventBus.getDefault().post(ExitUpdateActivityEvent())
            }
        }, {

        })

        viewModel.setupE2eIfNeeded()

        listenForDeviceIdChange()

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            val itemId: Int = item.itemId
            if (itemId == R.id.message) {
                selectedFragment = FragmentChats()
            } else if (itemId == R.id.status) {
                selectedFragment = StatusFragment()
            } else if (itemId == R.id.calls) {
                selectedFragment = CallsFragment()
            }
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment).commit()
            }
            true
        };
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FragmentChats()).commit()

    }


    private fun listenForDeviceIdChange() {
        FireConstants.deviceIdRef.child(FireManager.uid).observeValueEvent()
            .subscribe { dataSnapshot ->
                if (dataSnapshot.value != null && (dataSnapshot.value as? String) != DeviceId.id) {
                    startLoggedOutActivity()
                    FireManager.logout()
                    NotificationHelper(this@MainActivity).fireUserLoggedOutNotification()
                    finish()
                } else {
                }
            }.addTo(disposables)
    }


    override fun goingToUpdateActivity() {
        ignoreBatteryDialog?.dismiss()
        super.goingToUpdateActivity()
    }
    override fun onResume() {
        super.onResume()
        options = getOptionsByPreference(this)
    }

    private fun getOptionsByPreference(mActivity: MainActivity): Options {
        val sp = PreferenceManager.getDefaultSharedPreferences(mActivity)
        return Options().apply {
            isFrontFacing = sp.getBoolean("frontFacing", false)
            ratio = when (sp.getString("ratio", "0")) {
                "1" -> Ratio.RATIO_4_3
                "2" -> Ratio.RATIO_16_9
                else -> Ratio.RATIO_AUTO
            }
            flash = when (sp.getString("flash", "0")) {
                "1" -> Flash.Disabled
                "2" -> Flash.On
                "3" -> Flash.Off
                else -> Flash.Auto
            }
            mode = when (sp.getString("mode", "0")) {
                "1" -> Mode.Picture
                "2" -> Mode.Video
                else -> Mode.All
            }
            videoOptions = VideoOptions().apply {
                videoDurationLimitInSeconds = try {
                    sp.getString("videoDuration", "30")?.toInt() ?: 30
                } catch (e: Exception) {
                    sp.apply {
                        edit().putString("videoDuration", "30").apply()
                    }
                    30
                }
            }
            count = try {
                sp.getString("count", "5")?.toInt() ?: 5
            } catch (e: Exception) {
                sp.apply {
                    edit().putString("count", "5").apply()
                }
                5
            }
            spanCount = sp.getString("spanCount", "4")?.toInt() ?: 4
        }
    }


    //for users who updated the app
    private fun showPrivacyAlertDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setPositiveButton(R.string.agree_and_continue) { dialog, which ->
            SharedPreferencesManager.setAgreedToPrivacyPolicy(true)
        }

        alertDialog.setNegativeButton(R.string.cancel) { dialog, which ->
            finish()
        }

        alertDialog.show()
    }

    private fun showBatteryOptimizationDialog() {

        ignoreBatteryDialog = IgnoreBatteryDialog(this, R.style.AlertDialogTheme)

        ignoreBatteryDialog?.setOnDialogClickListener(object :
            IgnoreBatteryDialog.OnDialogClickListener {

            override fun onCancelClick(checkBoxChecked: Boolean) {
                SharedPreferencesManager.setDoNotShowBatteryOptimizationAgain(checkBoxChecked)
            }

            override fun onDismiss(checkBoxChecked: Boolean) {
                SharedPreferencesManager.setDoNotShowBatteryOptimizationAgain(checkBoxChecked)
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onOk() {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "could not open Battery Optimization Settings",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        })
        ignoreBatteryDialog?.show()
    }


    //start CameraActivity
    private fun startCamera() {
        Dexter.withContext(this)
            .withPermission(CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val intent = Intent(this@MainActivity, CameraActivity::class.java)
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
                        this@MainActivity,
                        R.string.missing_permissions,
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }).check()


    }




    override fun fetchStatuses() {
        users?.let {
            viewModel.fetchStatuses(it)
        }
    }


    private fun startServices() {
        if (!BuildVerUtil.isOreoOrAbove()) {
            startService(Intent(this, NetworkService::class.java))
            startService(Intent(this, InternetConnectedListener::class.java))
            startService(Intent(this, FCMRegistrationService::class.java))

        } else {
            if (!SharedPreferencesManager.isTokenSaved())
                SaveTokenJob.schedule(this, null)

            SetLastSeenJob.schedule(this)
            UnProcessedJobs.process(this)
        }

        //sync contacts for the first time
        if (!SharedPreferencesManager.isContactSynced()) {
            syncContacts()
        } else {
            //sync contacts every day if needed
            if (SharedPreferencesManager.needsSyncContacts()) {
                syncContacts()
            }
        }

        //schedule daily job to backup messages
        DailyBackupJob.schedule()

        StickerLoader(this).loadStickersIntoFilesDir()

        if (!SharedPreferencesManager.isDeviceIdSaved()) {
            viewModel.saveDeviceId()
        }

    }

    private fun syncContacts() {
        disposables.add(ContactUtils.syncContacts().subscribe({

        }, { throwable ->

        }))
    }


    private fun init() {
        toolbar = findViewById(R.id.toolbar)
        tvSelectedChatCount = findViewById(R.id.tv_selected_chat)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

    }



    override fun onPause() {
        super.onPause()
        ignoreBatteryDialog?.dismiss()
        fireListener?.cleanup()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val menuItem = menu.findItem(R.id.search_item)
        searchView = menuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                return false
            }

            //submit search for the current active fragment
            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.onQueryTextChange(newText)
                return false
            }

        })
        //revert back to original adapter
        searchView.setOnCloseListener {
            exitSearchMode()
            true
        }

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                return true
            }

            //exit search mode on searchClosed
            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                exitSearchMode()
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.settings_item -> {
                settingsItemClicked()
            }

            R.id.search_item -> {
                searchItemClicked()
            }

            R.id.new_person_item -> {
                startActivity(Intent(this@MainActivity, NewChatActivity::class.java))
            }

        }

        return super.onOptionsItemSelected(item)
    }


    private fun searchItemClicked() {
        isInSearchMode = true
    }


    private fun settingsItemClicked() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }


    override fun onBackPressed() {
        if (isInSearchMode)
            exitSearchMode()
        else {

            super.onBackPressed()
        }

    }


    fun exitSearchMode() {
        isInSearchMode = false
    }



    override fun onRotationAnimationEnd(drawable: Int) {
//        fab?.setImageResource(drawable)
//        animateTextStatusFab()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST || requestCode == ImageEditor.RC_IMAGE_EDITOR || requestCode == REQUEST_CODE_TEXT_STATUS) {
            viewModel.onActivityResult(requestCode, resultCode, data)

        }

    }


    override fun addMarginToFab(isAdShowing: Boolean) {
//        val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
//        val v = if (isAdShowing) DpUtil.toPixel(
//            95f,
//            this
//        ) else resources.getDimensionPixelSize(R.dimen.fab_margin).toFloat()
//
//
//        layoutParams.bottomMargin = v.toInt()
//
//        fab.layoutParams = layoutParams
//
//        fab.clearAnimation()
//        fab.animation?.cancel()

//        animateTextStatusFab()

    }


    override fun openCamera() {
        startCamera()
    }

    override fun startTheActionMode(callback: ActionMode.Callback) {
        startActionMode(callback)
    }

    /*private fun getFragmentByPosition(position: Int): Fragment? {
        return viewPager.currentItem?.let {
            supportFragmentManager.findFragmentByTagForViewPager(
                position,
                it
            )
        }
    }*/


    companion object {
        const val CAMERA_REQUEST = 9514
        const val REQUEST_CODE_TEXT_STATUS = 9145
        private const val CHATS_TAB_INDEX = 0

    }


}