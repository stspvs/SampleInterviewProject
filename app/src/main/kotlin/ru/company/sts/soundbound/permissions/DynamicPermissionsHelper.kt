package ru.company.sts.soundbound.permissions

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.MainThread
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.SparseArray
import ru.company.sts.soundbound.R

/**
 * Created by s.polozov on 25.01.2016.
 *
 *
 * Provides dynamic permissions functionality
 */
class DynamicPermissionsHelper : ActivityCompat.OnRequestPermissionsResultCallback {

    private val permissions: Array<String>

    private val requestExplanationMessage: String?
    private val requestGrantedListeners = SparseArray<IPermissionRequestResultListener>()
    private var forceShowExplanationMessage = false

    /**
     * @param activity    instance of activity which MUST implement ActivityCompat.OnRequestPermissionsResultCallback
     * @param permissions list of required permissions
     * @throws IllegalArgumentException - exception is thrown if activity does not implement ActivityCompat.OnRequestPermissionsResultCallback
     */
    @Throws(IllegalArgumentException::class)
    constructor(activity: Activity,
                permissions: Array<String>,
                requestExplanationMessage: String?) {
        if (activity is ActivityCompat.OnRequestPermissionsResultCallback) {
            this.permissions = permissions
            this.requestExplanationMessage = requestExplanationMessage
        } else {
            throw IllegalArgumentException("The activity MUST implement ActivityCompat.OnRequestPermissionsResultCallback")
        }
    }

    /**
     * @param activity   instance of activity which MUST implement ActivityCompat.OnRequestPermissionsResultCallback
     * @param permission required permission
     * @throws IllegalArgumentException - exception is throw if activity does not implement ActivityCompat.OnRequestPermissionsResultCallback
     */
    @Throws(IllegalArgumentException::class)
    constructor(activity: Activity,
                permission: String,
                requestExplanationMessage: String?) {
        if (activity is ActivityCompat.OnRequestPermissionsResultCallback) {
            this.permissions = arrayOf(permission)
            this.requestExplanationMessage = requestExplanationMessage
        } else {
            throw IllegalArgumentException("The activity MUST implement ActivityCompat.OnRequestPermissionsResultCallback")
        }
    }

    /**
     * @see DynamicPermissionsHelper.DynamicPermissionsHelper
     */
    constructor(permissions: Array<String>,
                requestExplanationMessage: String?) {
        this.permissions = permissions
        this.requestExplanationMessage = requestExplanationMessage
    }

    /**
     * @see DynamicPermissionsHelper.DynamicPermissionsHelper
     */
    constructor(permission: String,
                requestExplanationMessage: String?) {
        this.permissions = arrayOf(permission)
        this.requestExplanationMessage = requestExplanationMessage
    }

    /**
     * Shows user permissions request dialog with specified call back methods via
     * [IPermissionRequestResultListener] parameter
     *
     *
     * IMPORTANT: the activity provided in constructor must call [DynamicPermissionsHelper.onRequestPermissionsResult]
     * in implemented method [ActivityCompat.OnRequestPermissionsResultCallback]
     * if non-null requestResultListener is provided
     *
     * @param activity              activity implementing [ActivityCompat.OnRequestPermissionsResultCallback] interface
     * @param requestResultListener callback methods listener implementing
     * [IPermissionRequestResultListener.onAllPermissionsGranted] or
     * [IPermissionRequestResultListener.onAnyPermissionDenied]
     * @throws IllegalArgumentException if activity does not implementing [ActivityCompat.OnRequestPermissionsResultCallback]
     */
    @Throws(IllegalArgumentException::class)
    fun requestMissingPermissions(activity: Activity,
                                  requestCode: Int,
                                  requestResultListener: IPermissionRequestResultListener) {
        if (activity !is ActivityCompat.OnRequestPermissionsResultCallback) {
            throw IllegalArgumentException("The activity MUST implement ActivityCompat.OnRequestPermissionsResultCallback")
        }
        if (permissions.isNotEmpty()) {
            val mainLooper = Looper.getMainLooper()
            val handler = Handler(mainLooper)
            if (activity is IPermissionHelperActivity) {
                (activity as IPermissionHelperActivity).setPendingPermissionHelper(this)
            }
            if (Looper.myLooper() != mainLooper) {
                handler.post { requestPermissionsExplained(activity, requestCode, requestResultListener) }
            } else {
                requestPermissionsExplained(activity, requestCode, requestResultListener)
            }
        }
    }

    /**
     * @param forceShow true if permission request explanation message dialog should be shown always
     * before showing system runtime permission request dialog
     * false if system should be allowed to decide itself whether to show explanation message
     * or not ([Activity.shouldShowRequestPermissionRationale] will be used)
     *
     *
     * default value = false
     */
    fun setForceShowExplanationMessage(forceShow: Boolean) {
        this.forceShowExplanationMessage = forceShow
    }

    fun checkPermissionsAreGranted(context: Context): Boolean {
        return checkPermissionsAreGranted(context, permissions)
    }

    /**
     * If non null IPermissionRequestResultListener has been provided in
     * [DynamicPermissionsHelper.requestMissingPermissions]
     * then the method checks permission request result and depending on result it invokes
     * [IPermissionRequestResultListener.onAllPermissionsGranted] or
     * [IPermissionRequestResultListener.onAnyPermissionDenied]
     *
     *
     *
     *
     * IMPORTANT: Activity must call this method if IPermissionRequestResultListener has been provided
     *
     *
     * IMPORTANT: If more than one permission is requested at the same, then the method
     * calls ([IPermissionRequestResultListener.onAllPermissionsGranted] if ALL permissions have been granted
     * or invokes [IPermissionRequestResultListener.onAnyPermissionDenied]) if at least one permission denied
     */
    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val listener = requestGrantedListeners.get(requestCode)
        if (listener != null && permissions.isNotEmpty()) {
            var areAllPermissionsGranted = false
            if (grantResults.isNotEmpty()) {
                areAllPermissionsGranted = true
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        areAllPermissionsGranted = false
                        break
                    }
                }
            }
            if (areAllPermissionsGranted) {
                listener.onAllPermissionsGranted()
            } else {
                listener.onAnyPermissionDenied()
            }
            requestGrantedListeners.remove(requestCode)
            if (activity is IPermissionHelperActivity) {
                (activity as IPermissionHelperActivity).setPendingPermissionHelper(null)
            }
        }
    }

    /**
     * If non null IPermissionRequestResultListener has been provided in
     * [DynamicPermissionsHelper.requestMissingPermissions]
     * then the method checks permission request result and depending on result it invokes
     * [IPermissionRequestResultListener.onAllPermissionsGranted] or
     * [IPermissionRequestResultListener.onAnyPermissionDenied]
     *
     *
     *
     *
     * IMPORTANT: Activity must call this method if IPermissionRequestResultListener has been provided
     *
     *
     * IMPORTANT: If more than one permission is requested at the same, then the method
     * calls ([IPermissionRequestResultListener.onAllPermissionsGranted] if ALL permissions have been granted
     * or invokes [IPermissionRequestResultListener.onAnyPermissionDenied]) if at least one permission denied
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val listener = requestGrantedListeners.get(requestCode)
        if (listener != null) {
            var areAllPermissionsGranted = false
            if (grantResults.isNotEmpty()) {
                areAllPermissionsGranted = true
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        areAllPermissionsGranted = false
                        break
                    }
                }
            }
            if (areAllPermissionsGranted) {
                listener.onAllPermissionsGranted()
            } else {
                listener.onAnyPermissionDenied()
            }
            requestGrantedListeners.remove(requestCode)
        }
    }


    @MainThread
    private fun requestPermissionsExplained(activity: Activity,
                                            requestCode: Int,
                                            requestResultListener: IPermissionRequestResultListener?) {
        if ((forceShowExplanationMessage || ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0])) && requestExplanationMessage != null) {
            val onCancelListener = DialogInterface.OnCancelListener { _: DialogInterface -> requestResultListener?.onAnyPermissionDenied() }
            val onOkButtonClickListener = { _: DialogInterface, _: Int -> requestPermissions(activity, requestCode, requestResultListener) }
            val onCancelButtonClickListener = { dialog: DialogInterface, _: Int -> onCancelListener.onCancel(dialog) }
            val builder = AlertDialog.Builder(activity)
                    .setMessage(requestExplanationMessage)
                    .setCancelable(true)
                    .setOnCancelListener(onCancelListener)
                    .setPositiveButton(R.string.btnOk, onOkButtonClickListener)
                    .setNegativeButton(R.string.btnCancel, onCancelButtonClickListener)
            builder.show()
        } else {
            requestPermissions(activity, requestCode, requestResultListener)
        }
    }

    private fun requestPermissions(activity: Activity,
                                   requestCode: Int,
                                   requestResultListener: IPermissionRequestResultListener?) {
        if (requestResultListener != null) {
            requestGrantedListeners.put(requestCode, requestResultListener)
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }


    /**
     * IMPORTANT: avoid using Toast messages before/after permission requests in listeners
     * since it causes "Screen overlay detected" dialog preventing dynamic permission grants
     */
    interface IPermissionRequestResultListener {
        fun onAllPermissionsGranted()

        fun onAnyPermissionDenied()
    }

    interface IPermissionHelperActivity {
        fun setPendingPermissionHelper(pendingRequestHelper: DynamicPermissionsHelper?)
    }

    companion object {
        const val READ_EXTERNAL_STORAGE: String = "android.permission.READ_EXTERNAL_STORAGE"


        fun checkPermissionsAreGranted(context: Context, permissions: Array<String>): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val missingPermissions = ArrayList<String>()
                for (permission in permissions) {
                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(permission)
                    }
                }
                missingPermissions.size == 0
            } else {
                true
            }
        }
    }
}