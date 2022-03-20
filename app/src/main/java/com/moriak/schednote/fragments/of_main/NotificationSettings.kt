package com.moriak.schednote.fragments.of_main

import android.view.LayoutInflater
import android.view.ViewGroup
import com.moriak.schednote.databinding.AlarmsBinding
import com.moriak.schednote.enums.AlarmCategory
import com.moriak.schednote.fragments.ExtendedSubActivity

/**
 * Fragment má v sebe vnorené fragmenty súvisiace s upozorneniami
 */
class NotificationSettings: ExtendedSubActivity<AlarmsBinding>(AlarmCategory) {
    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        AlarmsBinding.inflate(inflater, container, false)
}