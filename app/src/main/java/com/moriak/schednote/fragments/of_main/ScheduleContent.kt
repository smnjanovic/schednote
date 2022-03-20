package com.moriak.schednote.fragments.of_main

import android.view.LayoutInflater
import android.view.ViewGroup
import com.moriak.schednote.databinding.ScheduleBinding
import com.moriak.schednote.enums.ScheduleDisplay
import com.moriak.schednote.fragments.ExtendedSubActivity

/**
 * Fragment má v sebe vnorené fragmenty súvisiace s rozvrhom.
 */
class ScheduleContent: ExtendedSubActivity<ScheduleBinding>(ScheduleDisplay) {
    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        ScheduleBinding.inflate(inflater, container, false)
}