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

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.ListOutcome;
import java.io.IOException;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.bitcoinj.wallet.Wallet;
import wtf.harvest.btcops.PendingRequests;
import wtf.harvest.btcops.Request;

/**
 * Postgres pending requests: reads and returns pending requests from given
 * data source.
 * @since 2.0
 * @todo #6:30min Integration test is missing for this class; please, implement
 *  it to check behaviour of this class.
 */
public final class PgPendingRequests implements PendingRequests {

    /**
     * Data base source.
     */
    private final DataSource database;

    /**
     * Ctor.
     * @param database Data base source
     */
    public PgPendingRequests(final DataSource database) {
        this.database = database;
    }

    @Override
    public Iterable<Request> get(final Wallet wallet) throws IOException {
        try {
            return new JdbcSession(this.database).sql(
                // @checkstyle LineLength (1 line)
                "SELECT ops.id, ops.amount, links.ref AS telegram FROM ioop_requests AS ops JOIN links ON ops.author = links.profile_id WHERE ops.name = 'deposit' AND ops.token = 'BTC' AND ops.status = 'pending' AND links.rel = 'telegram'"
            ).select(
                new ListOutcome<>(
                    rset -> new Request(
                        rset.getLong(1),
                        wallet.freshReceiveAddress(),
                        rset.getBigDecimal(2),
                        // @checkstyle MagicNumber (1 line)
                        rset.getLong(3)
                    )
                )
            );
        } catch (final SQLException ex) {
            throw new IOException("Failed to select pending requests", ex);
        }
    }
}
