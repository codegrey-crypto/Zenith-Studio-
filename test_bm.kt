import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build

fun test() {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.HARDWARE
        inMutable = true
    }
}
