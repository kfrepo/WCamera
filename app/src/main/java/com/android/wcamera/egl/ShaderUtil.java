package com.android.wcamera.egl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;

import com.android.wcamera.util.LogUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/22 15:53
 */
public class ShaderUtil {

    private static final String TAG = "ShaderUtil";

    public static String getRawResource(Context context, int rawId) {
        InputStream inputStream = context.getResources().openRawResource(rawId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer buffer = new StringBuffer();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    /**
     *  根据类型编译着色器
     * @param shaderType
     * @param source
     * @return
     */
    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);

            int[] compile = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0);
            if (compile[0] != GLES20.GL_TRUE) {
                LogUtil.e(TAG, "shader compile error!");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        if(vertexShader != 0 && fragmentShader != 0) {
            // 创建 OpenGL 程序 ID
            int program = GLES20.glCreateProgram();
            // 链接上 顶点着色器
            GLES20.glAttachShader(program, vertexShader);
            // 链接上 片段着色器
            GLES20.glAttachShader(program, fragmentShader);
            // 链接着色器之后，链接 OpenGL 程序
            GLES20.glLinkProgram(program);
            return program;
        }
        return 0;
    }

    public static Bitmap createTextImage(String text, int textSize, String textColor, String bgColor, int padding){
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        //设置是否抖动
        paint.setAntiAlias(true);

        float width = paint.measureText(text, 0, text.length());
        float top = paint.getFontMetrics().top;
        float bottom = paint.getFontMetrics().bottom;

        Bitmap bm = Bitmap.createBitmap((int) (width + padding * 2), (int) ((bottom - top) + padding * 2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);

        canvas.drawColor(Color.parseColor(bgColor));
        canvas.drawText(text, padding, - top + padding, paint);
        return bm;
    }

    public static int loadBitmapTexture(Bitmap bitmap){

        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ByteBuffer bitmapBuffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        //像素拷贝到Buffer
        bitmap.copyPixelsToBuffer(bitmapBuffer);
        bitmapBuffer.flip();

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
                0, GLES20.GL_RGBA,
                bitmap.getWidth(),
                bitmap.getHeight(),
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                bitmapBuffer);
        return textureIds[0];
    }

    public static Bitmap createLocalTimeImage(int textSize, String textColor, String bgColor, int padding){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String text = simpleDateFormat.format(new Date());

        Paint paint = new Paint();
        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        //设置是否抖动
        paint.setAntiAlias(true);

        float width = paint.measureText(text, 0, text.length());
        float top = paint.getFontMetrics().top;
        float bottom = paint.getFontMetrics().bottom;

        Bitmap bm = Bitmap.createBitmap((int) (width + padding * 2), (int) ((bottom - top) + padding * 2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);

        canvas.drawColor(Color.parseColor(bgColor));
        canvas.drawText(text, padding, - top + padding, paint);
        return bm;
    }

}
