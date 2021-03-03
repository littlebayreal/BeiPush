attribute vec4 position;
attribute vec4 inputTextureCoordinate;

// 高斯算子左右偏移值，当偏移值为5时，高斯算子为 11 x 11
const int SHIFT_SIZE = 5;

uniform highp float texelWidthOffset;
uniform highp float texelHeightOffset;

varying vec2 textureCoordinate;
varying vec4 blurShiftCoordinates[SHIFT_SIZE];
//uniform mat4 textureTransform;
void main() {
    gl_Position = position;
    textureCoordinate = inputTextureCoordinate.xy;
    // 偏移步距
    vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);
    // 记录偏移坐标
    for (int i = 0; i < SHIFT_SIZE; i++) {
        blurShiftCoordinates[i] = vec4(textureCoordinate.xy - float(i + 1) * singleStepOffset,
                                       textureCoordinate.xy + float(i + 1) * singleStepOffset);
    }
}