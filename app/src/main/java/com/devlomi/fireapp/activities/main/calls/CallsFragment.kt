package com.devlomi.fireapp.activities.main.calls

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.devlomi.fireapp.Advertisement.api.Constants
import com.devlomi.fireapp.Advertisement.api.RetrofitInstance
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.NewCallActivity
import com.devlomi.fireapp.activities.main.MainViewModel
import com.devlomi.fireapp.adapters.CallsAdapter
import com.devlomi.fireapp.fragments.BaseFragment
import com.devlomi.fireapp.interfaces.FragmentCallback
import com.devlomi.fireapp.model.Ads.Ads.AdsList
import com.devlomi.fireapp.model.realms.FireCall
import com.devlomi.fireapp.utils.ConnectionLiveData
import com.devlomi.fireapp.utils.PerformCall
import com.devlomi.fireapp.utils.RealmHelper
import com.devlomi.fireapp.utils.network.FireManager
import com.devlomi.hidely.hidelyviews.HidelyImageView
import com.google.android.gms.ads.AdView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_calls.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class CallsFragment : BaseFragment(), ActionMode.Callback, CallsAdapter.OnClickListener {

    override var adView: AdView? = null
    private lateinit var open_new_call_fab: FloatingActionButton
    private var fireCallList: RealmResults<FireCall>? = null
    private val selectedFireCallListActionMode: MutableList<FireCall> = ArrayList()
    private lateinit var adapter: CallsAdapter
    var listener: FragmentCallback? = null
    var actionMode: ActionMode? = null
    val fireManager = FireManager()
    private lateinit var cld: ConnectionLiveData

    val viewModel: MainViewModel by activityViewModels()
    override fun showAds(): Boolean {
        return resources.getBoolean(R.bool.is_calls_ad_enabled)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as FragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_calls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adView = view.findViewById(R.id.ad_view)
        open_new_call_fab = view.findViewById(R.id.open_new_call_fab)
        adViewInitialized(adView)
        initAdapter()
        checkNetworkConnection()

        viewModel.queryTextChange.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { newText ->
                onQueryTextChange(newText)
            })
        open_new_call_fab.setOnClickListener {
            startActivity(Intent(requireActivity(), NewCallActivity::class.java))
        }
    }

    private fun initAdapter() {
        fireCallList = RealmHelper.getInstance().allCalls
        adapter =
            CallsAdapter(fireCallList, selectedFireCallListActionMode, activity, this@CallsFragment)
        rv_calls.layoutManager = LinearLayoutManager(activity)
        rv_calls.adapter = adapter
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        this.actionMode = actionMode
        actionMode.menuInflater.inflate(R.menu.menu_action_calls, menu)
        actionMode.title = "1"
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        if (actionMode != null && menuItem != null) {
            if (menuItem.itemId == R.id.menu_item_delete) deleteClicked()
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
    }

    private fun deleteClicked() {
        val dialog = AlertDialog.Builder(requireActivity())
        dialog.setTitle(R.string.confirmation)
        dialog.setMessage(R.string.delete_calls_confirmation)
        dialog.setNegativeButton(R.string.no, null)
        dialog.setPositiveButton(R.string.yes) { dialogInterface, i ->
            for (fireCall in selectedFireCallListActionMode) {
                RealmHelper.getInstance().deleteCall(fireCall)
            }
            exitActionMode()
        }
        dialog.show()
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        this.actionMode = null
        selectedFireCallListActionMode.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun itemRemovedFromActionList(
        selectedCircle: HidelyImageView,
        itemView: View,
        fireCall: FireCall
    ) {
        selectedFireCallListActionMode.remove(fireCall)
        if (selectedFireCallListActionMode.isEmpty()) {
            actionMode?.finish()
        } else {
            selectedCircle.hide()
            itemView.setBackgroundColor(-1)
            actionMode?.title = selectedFireCallListActionMode.size.toString() + ""
        }
    }

    private fun itemAddedToActionList(
        selectedCircle: HidelyImageView,
        itemView: View,
        fireCall: FireCall
    ) {
        selectedCircle.show()
        itemView.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.item_selected_background_color
            )
        )
        selectedFireCallListActionMode.add(fireCall)
        actionMode?.title = selectedFireCallListActionMode.size.toString() + ""
    }

    fun exitActionMode() {
        actionMode?.finish()
    }


    override fun onQueryTextChange(newText: String?) {
        super.onQueryTextChange(newText)

        adapter?.filter(newText)

    }

    override fun onSearchClose() {
        super.onSearchClose()
        adapter =
            CallsAdapter(fireCallList, selectedFireCallListActionMode, activity, this@CallsFragment)
        rv_calls.adapter = adapter
    }

    override fun onItemClick(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        if (actionMode != null) {
            if (selectedFireCallListActionMode.contains(fireCall)) itemRemovedFromActionList(
                selectedCircle,
                itemView,
                fireCall
            ) else itemAddedToActionList(selectedCircle, itemView, fireCall)
        } else if (fireCall.user != null && fireCall.user.uid != null) PerformCall(
            requireActivity(),
            fireManager,
            disposables
        ).performCall(fireCall.isVideo, fireCall.user.uid)
    }

    override fun onIconButtonClick(view: View, fireCall: FireCall) {
        if (actionMode != null) return
        if (fireCall.user != null && fireCall.user.uid != null) PerformCall(
            requireActivity(),
            fireManager,
            disposables
        ).performCall(fireCall.isVideo, fireCall.user.uid)
    }

    override fun onLongClick(selectedCircle: HidelyImageView, itemView: View, fireCall: FireCall) {
        if (actionMode == null) {
            fragmentCallback?.startTheActionMode(this@CallsFragment)
            itemAddedToActionList(selectedCircle, itemView, fireCall)
        }
    }

    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(requireActivity().application)
        cld.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                RetrofitInstance.api.getAds(token = Constants.TOKEN).clone()
                    .enqueue(object : Callback<AdsList> {
                        override fun onResponse(call: Call<AdsList>, response: Response<AdsList>) {
                            adapter.let {
                                it.setAds(
                                    response.body()?.data
                                )
                                fireCallList?.let { list ->
                                    it.notifyItemRangeChanged(
                                        0,
                                        list.size
                                    )
                                }
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
                fireCallList?.let { list -> adapter.notifyItemRangeChanged(0, list.size) }
            }

        }
    }

}