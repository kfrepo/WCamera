#extension GL_OES_EGL_image_external : require
precision mediump float;
//采样点的坐标
varying vec2 ft_Position;
//采样器
uniform samplerExternalOES sTexture;
void main() {
    // texture2D：采样器 采集 ft_Position的像素
    gl_FragColor=texture2D(sTexture, ft_Position);
}
