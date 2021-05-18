/*
 * Copyright 2021 Swisscom Trust Services (Schweiz) AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swisscom.ais.client.model;

import java.io.File;

public class VisibleSignatureDefinition {

    public int x;
    public int y;
    public int width;
    public int height;
    public int page;

    public File icon;

    public VisibleSignatureDefinition(int x, int y,int width, int height, int page, File icon){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.page= page;
        this.icon = icon;
    }


}
