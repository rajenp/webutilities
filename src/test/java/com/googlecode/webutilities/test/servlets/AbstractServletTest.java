/*
 * Copyright 2010-2016 Rajendra Patil
 *
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

package com.googlecode.webutilities.test.servlets;

import com.googlecode.webutilities.test.AbstractWebComponentTest;
import com.mockrunner.servlet.ServletTestModule;

public abstract class AbstractServletTest extends AbstractWebComponentTest {

    protected ServletTestModule servletTestModule;

    @Override
    protected void setupInitParam(String name, String value) {
        webMockObjectFactory.getMockServletConfig().setInitParameter(name, value);
    }

    @Override
    protected void initModule() {
        this.servletTestModule = new ServletTestModule(this.webMockObjectFactory);
    }
}
