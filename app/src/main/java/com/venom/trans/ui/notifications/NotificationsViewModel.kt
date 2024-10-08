package com.venom.trans.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {
    private val mText: MutableLiveData<String> = MutableLiveData()

    init {
        mText.value = "This is notifications fragment"
    }

    val text: LiveData<String>
        get() = mText
}