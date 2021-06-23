package com.example.generatar

import android.app.Activity
import android.app.AlertDialog

class LoadingDialog internal constructor(var activity: Activity) {
    var dialog: AlertDialog? = null
    fun startloadingDialog() {
        var dialogue = dialog
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        builder.setView(inflater.inflate(R.layout.customdialogue, null))
        builder.setCancelable(true)
        dialogue = builder.create()
        dialogue.show()
    }

    fun dismissdialog() {
        dialog!!.dismiss()
    }
}