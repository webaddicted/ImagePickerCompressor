package com.webaddicted.imagepickercompressor.utils.constant

object AppConstant {
    const val SPLASH_DELAY: Long = 3000
    const val DISPLAY_DATE_FORMAT = "dd-MMM-yyyy"
    const val SERVER_DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_AM_PM_FORMAT = "hh:mm aaa"
    const val TIME_FORMAT = "hh:mm"
    const val DATE_TIME_FORMAT = "dd-MMM-yyyy hh:mm"
    const val IMG_FILE_NAME_FORMAT = "yyyyMMdd_HHmmss"
    const val IMG_FILE_EXT = "jpeg"
    const val IMGS_DIR = "app_imgs_dir"

    enum class WeekOff(var value: String) {
        SUNDAY("Sunday"),
        MONDAY("Monday"),
        TUESDAY("Tuesday"),
        WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"),
        FRIDAY("Friday"),
        SATURDAY("Saturday")
    }


}
