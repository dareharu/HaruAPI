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
package dareharu.haru.common.sql;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dareharu.haru.api.Haru;
import dareharu.haru.api.sql.SqlManager;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HaruSqlManager implements SqlManager, AutoCloseable {

    static final Map<String, Properties> PROTOCOL_SPECIFIC_PROPERTIES;

    static {
        final ImmutableMap.Builder<String, Properties> builder = ImmutableMap.builder();
        final Properties mySqlProps = new Properties();
        // Config options based on:
        // http://assets.en.oreilly.com/1/event/21/Connector_J%20Performance%20Gems%20Presentation.pdf
        mySqlProps.setProperty("useConfigs", "maxPerformance");
        builder.put("com.mysql.jdbc.Driver", mySqlProps);
        builder.put("org.mariadb.jdbc.Driver", mySqlProps);

        PROTOCOL_SPECIFIC_PROPERTIES = builder.build();
    }

    private LoadingCache<ConnectionInfo, HikariDataSource> connectionCache;

    @Inject
    HaruSqlManager(final Haru api) {
        this.buildConnectionCache();
    }

    public void buildConnectionCache() {
        this.connectionCache = null;
        this.connectionCache = Caffeine.newBuilder()
            .removalListener((RemovalListener<ConnectionInfo, HikariDataSource>) (key, value, cause) -> {
                if (value != null) {
                    value.close();
                }
            })
            .build(key -> {
                final HikariConfig config = new HikariConfig();
                config.setUsername(key.user());
                config.setPassword(key.password());
                config.setDriverClassName(key.driverClassName());
                // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing for info on pool sizing
                config.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() * 2) + 1);
                config.setLeakDetectionThreshold(60 * 1000);
                final Properties driverSpecificProperties =
                    HaruSqlManager.PROTOCOL_SPECIFIC_PROPERTIES.get(key.driverClassName());
                if (driverSpecificProperties != null) {
                    config.setDataSourceProperties(driverSpecificProperties);
                }
                config.setJdbcUrl(key.authlessUrl());

                return new HikariDataSource(config);
            });
    }

    @Override
    public DataSource dataSource(final String jdbcConnectionUrl) throws SQLException {
        final ConnectionInfo info = ConnectionInfo.fromUrl(jdbcConnectionUrl);

        try {
            return this.connectionCache.get(info);
        } catch (final CompletionException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.connectionCache != null) {
            this.connectionCache.invalidateAll();
        }
    }

    public static final class ConnectionInfo {

        private static final Pattern URL_REGEX = Pattern.compile("(?:jdbc:)?([^:]+):(//)?(?:([^:]+)(?::([^@]+))?@)?(.*)");
        private static final String UTF_8 = StandardCharsets.UTF_8.name();
        public final @Nullable String user;
        public final @Nullable String password;
        public final String driverClassName;
        public final String authlessUrl;
        public final String fullUrl;

        /**
         * Creates a new ConnectionInfo with the given parameters.
         *
         * @param user The username to use when connecting to the database
         * @param password The password to connect with. If user is not null, password must be null
         * @param driverClassName The class name of the driver to use for this connection
         * @param authlessUrl A JDBC url for this driver not containing authentication information
         * @param fullUrl The full JDBC url containing user, password, and database information
         */
        public ConnectionInfo(final @Nullable String user, final @Nullable String password, final String driverClassName, final String authlessUrl, final String fullUrl) {
            this.user = user;
            this.password = password;
            this.driverClassName = driverClassName;
            this.authlessUrl = authlessUrl;
            this.fullUrl = fullUrl;
        }

        public static ConnectionInfo fromUrl(final String fullUrl) throws SQLException {
            final Matcher matcher = ConnectionInfo.URL_REGEX.matcher(fullUrl);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("URL " + fullUrl + " is not a valid JDBC url");
            }

            final String protocol = matcher.group(1);
            final boolean hasSlashes = matcher.group(2) != null;
            final String user = ConnectionInfo.decodeUrl(matcher.group(3));
            final String password = ConnectionInfo.decodeUrl(matcher.group(4));
            final String specifier = matcher.group(5);

            final String unauthedUrl = "jdbc:" + protocol + (hasSlashes ? ":" : "://") + specifier;
            final String driverClassName = DriverManager.getDriver(unauthedUrl).getClass().getCanonicalName();

            return new ConnectionInfo(user, password, driverClassName, unauthedUrl, fullUrl);
        }

        public @Nullable String user() {
            return this.user;
        }

        public @Nullable String password() {
            return this.password;
        }

        public String driverClassName() {
            return this.driverClassName;
        }

        public String authlessUrl() {
            return this.authlessUrl;
        }

        public String fullUrl() {
            return this.fullUrl;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            final ConnectionInfo that = (ConnectionInfo) other;
            return Objects.equal(this.user, that.user)
                && Objects.equal(this.password, that.password)
                && Objects.equal(this.driverClassName, that.driverClassName)
                && Objects.equal(this.authlessUrl, that.authlessUrl)
                && Objects.equal(this.fullUrl, that.fullUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.user, this.password, this.driverClassName, this.authlessUrl, this.fullUrl);
        }

        private static String decodeUrl(final String str) {
            try {
                return str == null ? null : URLDecoder.decode(str, ConnectionInfo.UTF_8);
            } catch (final UnsupportedEncodingException e) {
                // If UTF-8 is not supported, we have bigger problems...
                throw new RuntimeException("UTF-8 is not supported on this system", e);
            }
        }

    }

}
