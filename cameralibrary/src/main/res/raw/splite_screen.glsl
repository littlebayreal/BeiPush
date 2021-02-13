#extension GL_OES_EGL_image_external : require

precision mediump float;

varying mediump vec2 textureCoordinate;

uniform samplerExternalOES inputImageTexture;

//void main() {
//    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
//}
void main(){
    vec2 uv = textureCoordinate.xy;
    float x;
    if (uv.x >= 0.0 && uv.x <= 0.5) {
        x = uv.x + 0.25;
    }else{
        x = uv.x - 0.25;
    }
    gl_FragColor = texture2D(inputImageTexture, vec2(x, uv.y));
}