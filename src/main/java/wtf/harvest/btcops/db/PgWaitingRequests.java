/*
 * Copyright (c) 2018 Harvest foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permissions is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package wtf.harvest.btcops.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import wtf.harvest.btcops.WaitingRequests;

/**
 * Implements {@link wtf.harvest.btcops.WaitingRequests} to retrieve waiting
 * requests from postgres db.
 * @since 2.0
 * @todo #6:30min Integration test is missing for this class; please, implement
 *  it to check behaviour of this class.
 */
public final class PgWaitingRequests implements WaitingRequests {

    /**
     * Data base source.
     */
    private final DataSource database;

    /**
     * Ctor.
     * @param database Data base source
     */
    public PgWaitingRequests(final DataSource database) {
        this.database = database;
    }

    @Override
    public Map<Long, String> get() throws IOException {
        final Map<Long, String> result = new HashMap<>();
        try (
            Connection connection = this.database.getConnection();
            PreparedStatement stmt = connection.prepareStatement(
                // @checkstyle LineLength (1 line)
                "SELECT request, address FROM ioop_btc_inputs WHERE status = 'waiting'"
            )
        ) {
            try (ResultSet query = stmt.executeQuery()) {
                while (query.next()) {
                    result.put(query.getLong(1), query.getString(2));
                }
            }
            return result;
        } catch (final SQLException err) {
            throw new IOException("Failed to get a waiting requests", err);
        }
    }
}
