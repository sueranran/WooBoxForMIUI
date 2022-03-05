package com.lt2333.simplicitytools.hook.app.systemui

import android.database.Cursor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lt2333.simplicitytools.util.XSPUtils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

class ControlCenterWeather : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        var mWeatherView: TextView? = null

        if (!XSPUtils.getBoolean("control_center_weather", false)) return

        var display_city = XSPUtils.getBoolean("control_center_weather_city", false)

        val classIfExists = XposedHelpers.findClassIfExists(
            "com.android.systemui.controlcenter.phone.widget.QSControlCenterHeaderView",
            lpparam.classLoader
        )

        XposedHelpers.findAndHookMethod(classIfExists, "onFinishInflate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val viewGroup = param.thisObject as ViewGroup

                val layout = XposedHelpers.findClass(
                    "androidx.constraintlayout.widget.ConstraintLayout\$LayoutParams",
                    lpparam.classLoader
                ).getConstructor(Int::class.java, Int::class.java).newInstance(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ) as ViewGroup.MarginLayoutParams

                XposedHelpers.setObjectField(
                    layout,
                    "bottomToTop",
                    viewGroup.context.resources.getIdentifier(
                        "date_time",
                        "id",
                        viewGroup.context.packageName
                    )
                )

                XposedHelpers.setObjectField(
                    layout,
                    "startToEnd",
                    viewGroup.context.resources.getIdentifier(
                        "big_time",
                        "id",
                        viewGroup.context.packageName
                    )
                )

                layout.marginStart = viewGroup.context.resources.getDimensionPixelSize(
                    viewGroup.context.resources.getIdentifier(
                        "notification_panel_time_date_space",
                        "dimen",
                        viewGroup.context.packageName
                    )
                )

                mWeatherView = TextView(viewGroup.context).also {
                    it.setTextAppearance(
                        viewGroup.context.resources.getIdentifier(
                            "TextAppearance.QSControl.Date",
                            "style",
                            viewGroup.context.packageName
                        )
                    )
                    it.layoutParams = layout
                }

                viewGroup.addView(mWeatherView)

                val query: Cursor? = viewGroup.context.contentResolver
                    .query(Uri.parse("content://weather/weather"), null, null, null, null)
                var str = "未找到天气"

                class T : TimerTask() {
                    override fun run() {
                        try {
                            if (query != null) {
                                if (query.moveToFirst()) {
                                    if (display_city) {
                                        str =
                                            query.getString(query.getColumnIndexOrThrow("city_name")) + " " +
                                                    query.getString(query.getColumnIndexOrThrow("description")) + " " +
                                                    query.getString(query.getColumnIndexOrThrow("temperature"))
                                    } else {
                                        str =
                                            query.getString(query.getColumnIndexOrThrow("description")) + " " +
                                                    query.getString(query.getColumnIndexOrThrow("temperature"))
                                    }

                                }
                                query.close()
                            }
                            mWeatherView!!.text = str
                        } catch (e: Exception) {
                        }
                    }
                }
                //每小时刷新一次
                Timer().scheduleAtFixedRate(
                    T(),
                    0,
                    3600000
                )

            }
        })
        //解决横屏重叠
        XposedHelpers.findAndHookMethod(classIfExists, "updateLayout", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val mOrientation =
                    XposedHelpers.getObjectField(param.thisObject, "mOrientation") as Int
                if (mOrientation == 1) {
                    mWeatherView!!.visibility = View.VISIBLE
                } else {
                    mWeatherView!!.visibility = View.GONE
                }
            }
        })
    }
}