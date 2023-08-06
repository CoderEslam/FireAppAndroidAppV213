package com.devlomi.fireapp.activities.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.SplashActivity
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.model.API.Login.CallbackLogin
import com.devlomi.fireapp.model.API.Login.LoginPhone
import com.devlomi.fireapp.model.Ads.SessionManger.getPhone
import com.devlomi.fireapp.model.Ads.SessionManger.setID
import com.devlomi.fireapp.model.Ads.SessionManger.setPhone
import com.devlomi.fireapp.model.Ads.SessionManger.setToken
import com.devlomi.fireapp.utils.network.AuthManager
import kotlinx.android.synthetic.main.activity_authentication.*
import kotlinx.android.synthetic.main.fragment_enter_phone_number.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthenticationActivity : AppCompatActivity(), AuthCallbacks {

    private val TAG = "AuthenticationActivity"
    private val authManager = AuthManager()

    private val viewModel: AuthenticationViewModel by viewModels()

    private lateinit var navigation: NavController

    lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        navigation = Navigation.findNavController(this, R.id.nav_host_fragment)
        navigation.setGraph(R.navigation.nav_signup)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment


        subscribeObservers()


    }

    private fun subscribeObservers() {
        viewModel.goToEnterVerifyPhoneFragment.observe(this) {
            navigation.navigate(R.id.action_enterPhoneNumberFragment_to_verifyPhoneFragment, it)
        }

        viewModel.showLoading.observe(this) {
            setLoading(it)
        }

        viewModel.goToAuthActivity.observe(this) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
        }

        viewModel.showMessage.observe(this) {
            AlertDialog.Builder(this).apply {
                setMessage(it)
                setPositiveButton(R.string.ok, null)
                show()
            }
        }

        viewModel.verify.observe(this) {
            val (number, callbacks) = it
            authManager.verify(number, this, callbacks)
        }

        viewModel.register.observe(this) {
            loginOrRegister(it)
        }


    }

    private fun loginOrRegister(uid: String) {
        RetrofitInstance.api.registerWithPhone(
            LoginPhone(
                getPhone().toString(),
                getPhone().toString(),
                uid
            )
        )
            .clone()
            .enqueue(object : Callback<CallbackLogin> {
                override fun onResponse(
                    call: Call<CallbackLogin>,
                    response: Response<CallbackLogin>
                ) {
                    if (response.body() == null) {
                        RetrofitInstance.api.login(
                            LoginPhone(
                                getPhone().toString(),
                                getPhone().toString(),
                                uid
                            )
                        ).clone()
                            .enqueue(object : Callback<CallbackLogin> {
                                override fun onResponse(
                                    call: Call<CallbackLogin>,
                                    response: Response<CallbackLogin>
                                ) {
                                    Log.e(
                                        TAG,
                                        "onResponse Callback Login : ${response.body().toString()}"
                                    )
                                    try {
                                        setToken(response.body()!!.user!!.device_token!!)
                                        setID(response.body()!!.user!!.id.toString())
                                        setPhone(response.body()!!.user!!.phone!!)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Exception Login: ${e.message}")
                                    }
                                }

                                override fun onFailure(call: Call<CallbackLogin>, t: Throwable) {
                                    Log.e(TAG, "onFailure: ${t.message}")
                                }

                            })
                    } else {
                        Log.e(TAG, "onResponse Callback Register : ${response.body().toString()}")
                        try {
                            setToken(response.body()!!.user!!.device_token!!)
                            setID(response.body()!!.user!!.id.toString())
                            setPhone(response.body()!!.user!!.phone!!)
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception Register: ${e.message}")
                        }
                    }
                }

                override fun onFailure(call: Call<CallbackLogin>, t: Throwable) {

                }

            })
    }

    override fun verifyPhoneNumber(phoneNumber: String, countryCode: String) {
        viewModel.verifyPhoneNumber(phoneNumber, countryCode)
    }


    override fun verifyCode(code: String) {
        viewModel.verifyCode(code)
    }

    override fun cancelVerificationRequest() {
        viewModel.cancelVerification()
    }


    private fun setLoading(setLoading: Boolean) {
        progressbar.isVisible = setLoading

        navHostFragment.childFragmentManager.fragments.getOrNull(0)?.let { fragment ->
            if (fragment is BaseAuthFragment) {
                if (setLoading)
                    fragment.disableViews()
                else
                    fragment.enableViews()

            }
        }


    }

    override fun onStart() {
        super.onStart()
        if (viewModel.isLoggedIn()) {
            startSplashActivity()
        }

    }

    private fun startSplashActivity() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }


}