
#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CameraView {
    float Grayness;
    float Darkness;
    float Focus;
    float Zoom;
    float DistortionTime;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 safeSize = max(InSize, vec2(1.0));

    float focus = clamp(Focus, 0.0, 1.0);
    float zoom = clamp(Zoom, 1.0, 3.0);

    float pixelCount = mix(160.0, 720.0, focus);
    pixelCount /= zoom;

    vec2 pixelGrid = vec2(
        pixelCount,
        pixelCount * (safeSize.y / safeSize.x)
    );

    vec2 zoomedUv =
        (texCoord - vec2(0.5)) / zoom + vec2(0.5);

    zoomedUv = clamp(
        zoomedUv,
        vec2(0.0),
        vec2(1.0)
    );

    vec2 sampleUv = (
        floor(zoomedUv * pixelGrid) + vec2(0.5)
    ) / pixelGrid;

    sampleUv = clamp(
        sampleUv,
        vec2(0.5) / safeSize,
        vec2(1.0) - vec2(0.5) / safeSize
    );

    vec3 sourceColor = texture(InSampler, sampleUv).rgb;

    float chromaCompressed = mix(
        min(sourceColor.r, min(sourceColor.g, sourceColor.b)),
        max(sourceColor.r, max(sourceColor.g, sourceColor.b)),
        0.4
    );

    float luminance = dot(
        sourceColor,
        vec3(0.2126, 0.7152, 0.0722)
    );

    float gray = mix(
        chromaCompressed,
        luminance,
        clamp(Grayness, 0.0, 1.0)
    );

    float contrast = clamp(
        (gray - 0.5) * 1.22 + 0.5,
        0.0,
        1.0
    );

    float tone = pow(contrast, 1.08)
        * (1.0 - clamp(Darkness, 0.0, 0.8));

    vec2 centered = (texCoord - vec2(0.5))
        * vec2(1.0, safeSize.y / safeSize.x);

    float radius = length(centered);

    float vignette = 1.0 - 0.24
        * smoothstep(0.28, 0.78, radius);

    float scanline = 0.992
        + 0.008 * sin(texCoord.y * safeSize.y * 3.14159265);

    float finalTone = clamp(
        tone * vignette * scanline,
        0.0,
        1.0
    );

    fragColor = vec4(vec3(finalTone), 1.0);
}
