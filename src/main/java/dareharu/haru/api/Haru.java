/*
 * This file is part of HaruAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) Dareharu <https://github.com/dareharu>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dareharu.haru.api;

import dagger.Component;
import dareharu.haru.api.sql.SqlManager;
import dareharu.haru.common.inject.SqlManagerModule;

import javax.inject.Singleton;

@Singleton
@Component(
    modules = {
        SqlManagerModule.class
    }
)
public interface Haru {

    /** Represents the {@link Haru} version. */
    String VERSION = "@version@";

    /**
     * Gets the {@link Haru} version satisfying semver
     *
     * @return The version satisfying semver
     */
    default String version() {
        return VERSION;
    }

    /**
     * Gets the {@link SqlManager} for grabbing sql data sources.
     *
     * @return The sql manager
     */
    SqlManager sqlManager();

}
