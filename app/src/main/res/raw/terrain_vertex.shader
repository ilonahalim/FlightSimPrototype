/*
 * Copyright 2017 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

uniform mat4 u_Model;
uniform mat4 u_MVMatrix;
uniform mat4 u_MVPMatrix;
uniform vec3 u_LightPos;

attribute vec4 a_Position;      // Per-vertex position information we will pass in.
attribute vec4 a_Color;         // Per-vertex color information we will pass in.
attribute vec3 a_Normal;        // Per-vertex normal information we will pass in.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.

varying vec3 v_Position;        // This will be passed into the fragment shader.
varying vec4 v_Color;           // This will be passed into the fragment shader.
varying vec3 v_Normal;          // This will be passed into the fragment shader.
varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.

// The entry point for our vertex shader.
void main()
{
    // Transform the vertex into eye space.
    v_Position = vec3(a_Position);

    // Pass through the color.
    v_Color = a_Color;

    // Pass through the texture coordinate.
    v_TexCoordinate = a_TexCoordinate;

    // Transform the normal's orientation into eye space.
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix * a_Position;
}