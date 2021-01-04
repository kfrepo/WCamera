package com.android.wcamera.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.android.wcamera.R;
import com.android.wcamera.egl.ShaderUtil;
import com.android.wcamera.egl.WEGLSurfaceView;
import com.android.wcamera.util.DisplayUtil;
import com.android.wcamera.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/23 10:57
 */
public class CameraRender implements WEGLSurfaceView.GLRender, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraRender";

    private Context context;

    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };
    private FloatBuffer vertexBuffer;

    private float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    private FloatBuffer fragmentBuffer;

    private int program;
    private int vPosition;
    private int fPosition;
    private int vboId;
    private int fboId;

    private int fboTextureid;
    private int cameraTextureid;

    private SurfaceTexture surfaceTexture;
    private OnSurfaceCreateListener onSurfaceCreateListener;

    private CameraFboRender cameraFboRender;

    private int umatrix;
    private float[] matrix = new float[16];
    private int screenWidth;
    private int screenHeight;

    private int width;
    private int height;

    public CameraRender(Context context) {
        this.context = context;

        screenWidth = DisplayUtil.getScreenWidth(context);
        screenHeight = DisplayUtil.getScreenHeight(context);

        cameraFboRender = new CameraFboRender(context);
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);
        LogUtil.d(TAG, "init vertexBuffer fragmentBuffer success!");
    }

    public void setOnSurfaceCreateListener(OnSurfaceCreateListener onSurfaceCreateListener) {
        this.onSurfaceCreateListener = onSurfaceCreateListener;
    }

    @Override
    public void onSurfaceCreated() {

        cameraFboRender.onCreate();
        String vertexSource = ShaderUtil.getRawResource(context, R.raw.vertex_shader);
        String fragmentSource = ShaderUtil.getRawResource(context, R.raw.fragment_shader);

        program = ShaderUtil.createProgram(vertexSource, fragmentSource);
        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");
        umatrix = GLES20.glGetUniformLocation(program, "u_Matrix");

        //vbo
        int [] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20. GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //创建fbo
        int[] fbos = new int[1];
        GLES20.glGenBuffers(1, fbos, 0);
        fboId = fbos[0];
        //绑定fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);

        //创建纹理
        int []textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        fboTextureid = textureIds[0];

        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureid);

        //环绕方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //过滤方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //fbo分配空间
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, screenWidth, screenHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        //绑定纹理和fbo
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureid, 0);
        if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            LogUtil.e(TAG, "fbo wrong");
        } else {
            LogUtil.d(TAG, "fbo success");
        }

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //解绑fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //创建Camera纹理
        int []textureidseos = new int[1];
        GLES20.glGenTextures(1, textureidseos, 0);
        cameraTextureid = textureidseos[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureid);

        //环绕 过滤方式
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //绑定到SurfaceTexture
        surfaceTexture = new SurfaceTexture(cameraTextureid);
        surfaceTexture.setOnFrameAvailableListener(this);

        if(onSurfaceCreateListener != null) {
            onSurfaceCreateListener.onSurfaceCreate(surfaceTexture, fboTextureid);
        }
        //解绑纹理,
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        LogUtil.d(TAG, "onSurfaceCreated success!");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
//        cameraFboRender.onChange(width, height);
//        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        LogUtil.d(TAG, "onSurfaceChanged : " + width + " " + height);
    }

    public void resetMatrix() {
        //初始化矩阵
        Matrix.setIdentityM(matrix, 0);
    }

    /**
     * 矩阵旋转
     * @param angle
     * @param x
     * @param y
     * @param z
     */
    public void setAngle(float angle, float x, float y, float z) {
        LogUtil.d(TAG, "矩阵旋转 angle:" + angle + " x:" + x + " y:" + y + " z:" + z);
        Matrix.rotateM(matrix, 0, angle, x, y, z);
    }

    @Override
    public void onDrawFrame() {
//        LogUtil.d(TAG, "onDraw!");
        surfaceTexture.updateTexImage();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f,0f, 0f, 1f);

        GLES20.glUseProgram(program);

        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        GLES20.glUniformMatrix4fv(umatrix, 1, false, matrix, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //取顶点坐标
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8,
                0);
        //取纹理坐标
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8,
                vertexData.length * 4);

        //绘制坐标系
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //解绑vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        //解绑fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        cameraFboRender.onChange(width, height);
        cameraFboRender.onDraw(fboTextureid);
    }

    /**
     * 摄像头数据返回
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        LogUtil.d(TAG, "onFrameAvailable!");
    }

    public interface OnSurfaceCreateListener {
        void onSurfaceCreate(SurfaceTexture surfaceTexture, int textureId);
    }

    public int getFboTextureid() {
        return fboTextureid;
    }

}
