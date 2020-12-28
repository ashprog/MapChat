package in.ashprog.mapchat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class RescaleImage {

    static Bitmap getRoundedResizedBitmap(Bitmap bitmap) {

        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);

        int widthLight = resizeBitmap.getWidth();
        int heightLight = resizeBitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(resizeBitmap.getWidth(), resizeBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paintColor = new Paint();
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF(new Rect(0, 0, widthLight, heightLight));

        canvas.drawRoundRect(rectF, widthLight / 2, heightLight / 2, paintColor);

        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(resizeBitmap, 0, 0, paintImage);

        return output;
    }

    static Bitmap addCircularBorder(Bitmap srcBitmap, int borderColor) {
        int borderWidth = 12;
        int outputWidth = srcBitmap.getWidth() + borderWidth * 2;

        Bitmap output = Bitmap.createBitmap(outputWidth, outputWidth, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        canvas.drawBitmap(srcBitmap, borderWidth, borderWidth, null);

        Paint paint = new Paint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        paint.setAntiAlias(true);

        canvas.drawCircle(
                canvas.getWidth() / 2, // cx
                canvas.getWidth() / 2, // cy
                canvas.getWidth() / 2 - borderWidth / 2,
                paint
        );

        return output;
    }
}
