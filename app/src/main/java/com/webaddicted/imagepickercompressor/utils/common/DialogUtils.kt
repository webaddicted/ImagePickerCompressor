package com.webaddicted.imagepickercompressor.utils.common

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spanned
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.webaddicted.imagepickercompressor.R

class DialogUtils {
    companion object {

        fun fullScreenTransDialogBounds(activity: Activity, dialog: Dialog?) {
            if (dialog != null && dialog.window != null) {
                dialog.window?.setBackgroundDrawable(
                    ColorDrawable(
                        ContextCompat.getColor(
                            activity,
                            R.color.transparent
                        )
                    )
                )
                dialog.window?.decorView?.background = ColorDrawable(Color.TRANSPARENT)
//        val width = ViewGroup.LayoutParams.MATCH_PARENT
//        val height = ((dialog.context.resources.displayMetrics.heightPixels * 0.55).roundToInt())
//        dialog.window?.setLayout(width, height)
                val lp = WindowManager.LayoutParams()
                val window = dialog.window
                lp.copyFrom(window?.attributes)
                //This makes the dialog take up the full width
                //lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                //      lp.width = (int) (dialog.getContext().getResources().getDisplayMetrics().widthPixels * 0.83);
//         lp.height = ((dialog.context.resources.displayMetrics.heightPixels * 0.55).roundToInt())
                window?.attributes = lp
            }
        }

        fun fullScreenTransDialog(
            activity: Activity, dialog: Dialog?, percentWidth: Double = 0.87,
            percentHeight: Double? = 0.90
        ) {
            if (dialog != null && dialog.window != null) {
                dialog.window?.setBackgroundDrawable(
                    ColorDrawable(
                        ContextCompat.getColor(
                            activity,
                            R.color.transparent
                        )
                    )
                )

                dialog.window?.decorView?.background = ColorDrawable(Color.TRANSPARENT)
                val displayMetrics = activity.resources.displayMetrics
                val dialogWidth = (percentWidth * displayMetrics.widthPixels).toInt()
                val dialogHeight = if (percentHeight != null)
                    (percentHeight * displayMetrics.heightPixels).toInt()
                else
                    ViewGroup.LayoutParams.WRAP_CONTENT
                dialog.window?.setLayout(dialogWidth, dialogHeight)
////        val width = ViewGroup.LayoutParams.MATCH_PARENT
////        val height = ((dialog.context.resources.displayMetrics.heightPixels * 0.55).roundToInt())
////        dialog.window?.setLayout(width, height)
//        val lp = WindowManager.LayoutParams()
//        val window = dialog.window
//        lp.copyFrom(window?.attributes)
//        //This makes the dialog take up the full width
//        //lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//        //      lp.width = (int) (dialog.getContext().getResources().getDisplayMetrics().widthPixels * 0.83);
////         lp.height = ((dialog.context.resources.displayMetrics.heightPixels * 0.55).roundToInt())
//        window?.attributes = lp
            }
        }

        fun getDialogInstance(
            activity: Activity,
            title: String,
            message: Spanned,
            button: String,
            okListener: DialogInterface.OnClickListener
        ): Dialog {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message).setPositiveButton(button) { dialog, id ->
                okListener.onClick(dialog, id)
            }
            return builder.create()
        }

        fun showDialog(
            activity: Activity,
            title: String = "",
            message: String = "",
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            if (title.isNotEmpty())
                builder.setTitle(title)
            builder.setMessage(message)
                .setPositiveButton(
                    activity.getString(R.string.ok)
                ) { dialog, id ->
                }
            builder.create()
            builder.show()
            return builder
        }

        fun getSingleClickDialogInstance(
            activity: Activity,
            title: String = "",
            message: String = "",
            button: String = "Ok",
            listener: DialogInterface.OnClickListener? = null
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
                .setPositiveButton(
                    button
                ) { dialog, id ->
                    listener?.onClick(dialog, id)
                }
            builder.create()
            builder.show()
            return builder
        }

        fun getDialogInstance(
            activity: Activity,
            title: String,
            message: String?,
            okBtn: String,
            cancelBtn: String,
            okClickListener: DialogInterface.OnClickListener?,
            cancelListener: DialogInterface.OnClickListener?
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
                .setPositiveButton(
                    okBtn
                ) { dialog, id ->
                    okClickListener?.onClick(dialog, id)
                }
                .setNegativeButton(
                    cancelBtn
                ) { dialog, id ->
                    cancelListener?.onClick(dialog, id)
                }
            builder.create()
            builder.show()
            return builder
        }

        fun getDialogInstance(
            activity: Activity,
            title: String,
            message: String,
            btnOk: String,
            btnCancel: String,
            btnNeutral: String,
            okClickListener: DialogInterface.OnClickListener,
            cancelListener: DialogInterface.OnCancelListener,
            neutralListener: DialogInterface.OnDismissListener
        ): Dialog {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
                .setPositiveButton(btnOk) { dialog, id ->
                    okClickListener.onClick(dialog, id)
                }
                .setNegativeButton(btnCancel) { dialog, id ->
                    cancelListener.onCancel(dialog)
                }
                .setNeutralButton(btnNeutral) { dialog, id ->
                    neutralListener.onDismiss(dialog)
                }
            return builder.create()
        }

        fun <T> getSingleChoiceDialog(
            context: Context,
            title: String,
            items: List<T>,
            okListener: DialogInterface.OnClickListener,
            cancelListener: DialogInterface.OnClickListener
        ): AlertDialog {
            return showSingleChoiceDialog(
                context,
//        R.style.AlertDialogStyle,
//        R.style.DialogSlideUpAnimation,
                title,
                items,
                0,
                context.resources.getString(R.string.ok),
                context.resources.getString(R.string.cancel),
                okListener,
                cancelListener
            )
        }

        fun <T> showSingleChoiceDialog(
            context: Context,
//      style: Int,
//      dialogAnimation: Int,
            title: String?,
            items: List<T>,
            checkedItem: Int,
            okBtn: String,
            cancelBtn: String,
            okListener: DialogInterface.OnClickListener,
            cancelListener: DialogInterface.OnClickListener
        ): AlertDialog {
            val size = items.size
            val itemArray = arrayOfNulls<String>(size)
            for (i in 0 until size) {
                itemArray[i] = items[i].toString()
            }
            val builder = AlertDialog.Builder(context)
            if (title != null) builder.setTitle(title)
            builder.setSingleChoiceItems(itemArray, checkedItem) { dialog, which ->
                okListener.onClick(dialog, which)
            }
            builder.setPositiveButton(okBtn, okListener)
            builder.setNegativeButton(cancelBtn, cancelListener)
            val alertDialog = builder.create()
//      alertDialog.window?.attributes?.windowAnimations = dialogAnimation
            alertDialog.show()
            return alertDialog
        }

        fun forceUpdateDialog(
            activity: Activity,
            title: String = "",
            message: String? = "",
            button: String = "Update Now",
            listener: DialogInterface.OnClickListener? = null
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
                .setPositiveButton(
                    button
                ) { dialog, id ->
                    listener?.onClick(dialog, id)
                }
            builder.setCancelable(false)
            builder.create()
            builder.show()
            return builder
        }
    }

}