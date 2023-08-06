package com.devlomi.fireapp.activities.authentication

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.devlomi.fireapp.R
import com.devlomi.fireapp.api.Constants.PhoneAlreadyTaken
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.model.API.Login.CallbackLogin
import com.devlomi.fireapp.model.API.Login.LoginPhone
import com.devlomi.fireapp.model.Ads.SessionManger.setID
import com.devlomi.fireapp.model.Ads.SessionManger.setPhone
import com.devlomi.fireapp.model.Ads.SessionManger.setToken
import com.devlomi.fireapp.utils.MyApp
import com.devlomi.fireapp.utils.NetworkHelper
import com.devlomi.fireapp.utils.Util
import kotlinx.android.synthetic.main.fragment_enter_phone_number.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EnterPhoneNumberFragment : BaseAuthFragment() {

    private val TAG = "EnterPhoneNumberFragmen"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_enter_phone_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cp.setDefaultCountryUsingNameCode("US")
        cp.detectSIMCountry(true)




        btn_verify.setOnClickListener {
            val number = et_number.text.toString().trim()
            val fullNumber = cp.selectedCountryCodeWithPlus + number


            //dismiss keyboard
            et_number.onEditorAction(EditorInfo.IME_ACTION_DONE)


            AlertDialog.Builder(requireActivity()).apply {
                val message = requireActivity().getString(
                    R.string.enter_phone_confirmation_message,
                    fullNumber
                )
                setMessage(message)
                setNegativeButton(R.string.edit, null)
                setPositiveButton(R.string.ok) { _, _ ->
                    //check for internet connection
                    if (NetworkHelper.isConnected(MyApp.context())) {

                        if (TextUtils.isEmpty(et_number.text) || TextUtils.isDigitsOnly(et_number.text)
                                .not()
                        )
                            Util.showSnackbar(
                                requireActivity(),
                                requireActivity().getString(R.string.enter_correct_number)
                            )
                        else {
                            callbacks?.verifyPhoneNumber(number, cp.selectedCountryNameCode)
                            requireActivity().setPhone(fullNumber)

                        }

                    } else {
                        Util.showSnackbar(
                            requireActivity(),
                            requireActivity().getString(R.string.no_internet_connection)
                        )
                    }
                }
                show()
            }
        }

    }


    override fun enableViews() {
        super.enableViews()
        et_number.isEnabled = true
        btn_verify.isEnabled = true
    }

    override fun disableViews() {
        super.disableViews()
        et_number.isEnabled = false
        btn_verify.isEnabled = false
    }


}