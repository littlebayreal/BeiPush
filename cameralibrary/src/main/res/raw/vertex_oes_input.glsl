// GL_OES_EGL_image_external 格式纹理输入滤镜，其中transformMatrix是SurfaceTexture的transformMatrix
uniform mat4 transformMatrix;
attribute vec4 position;
attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

void main() {
    gl_Position = position;
    textureCoordinate = (transformMatrix * inputTextureCoordinate).xy;
}
