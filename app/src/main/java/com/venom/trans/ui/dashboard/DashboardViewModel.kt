package com.venom.trans.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {
    private val mText: MutableLiveData<String> = MutableLiveData()

    init {
        mText.value = "This is dashboard fragment"
    }

    val text: LiveData<String>
        get() = mText
}