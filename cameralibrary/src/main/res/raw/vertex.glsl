attribute vec4 position;
attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

uniform mat4 textureTransform;

void main() {
//    vec4 temp = vec4(inputTextureCoordinate.xy,0.0,1.0);
    textureCoordinate = (textureTransform * inputTextureCoordinate).xy;
    gl_Position = position;
//    textureCoordinate = vec2(inputTextureCoordinate.x,inputTextureCoordinate.y);
}
