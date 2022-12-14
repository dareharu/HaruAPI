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
package dareharu.haru.api.sql;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * This manager provides the basics for an abstraction over SQL connection.
 */
public interface SqlManager {

    /**
     * Returns a data source for the provided JDBC connection string.
     *
     * <p>A jdbc connection url is expected to be of the form:<br>
     * jdbc:&lt;engine&gt;://[&lt;username&gt;[:&lt;password&gt;]@]&lt;
     * host&gt;/&lt;database&gt;.</p>
     *
     * @param jdbcConnectionUrl The jdbc url
     * @return A data source providing connections to the given URL
     * @throws SQLException If a connection to the given database could not be established
     */
    DataSource dataSource(final String jdbcConnectionUrl) throws SQLException;

}
