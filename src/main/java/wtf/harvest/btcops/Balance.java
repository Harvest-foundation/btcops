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
package wtf.harvest.btcops;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;
import com.jcabi.jdbc.SingleOutcome;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.Base64;
import javax.sql.DataSource;
import org.cactoos.crypto.digest.DigestFrom;
import org.cactoos.crypto.digest.DigestOf;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.cactoos.io.UncheckedBytes;

/**
 * Balance.
 *
 * @since 2.0
 */
final class Balance {

    /**
     * Request rid.
     */
    private final long rid;

    /**
     * Amount.
     */
    private final BigDecimal amount;

    /**
     * Ctor.
     * @param rid Request rid
     * @param amount Value
     */
    Balance(final long rid, final BigDecimal amount) {
        this.rid = rid;
        this.amount = amount;
    }

    /**
     * Accept btc operation.
     * @param data Db source
     * @throws SQLException If smth went wrong
     */
    public void accept(final DataSource data) throws SQLException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final Long author =
            session.sql("SELECT author FROM ioop_requests WHERE rid = ?")
                .set(this.rid)
                .select(new SingleOutcome<>(Long.class));
        // @checkstyle LineLengthCheck (1 line)
        session.sql("INSERT INTO ledger (src, dst, amount, details, token) VALUES (?, ?, ?,?, ?)")
            .prepare(
                stmt -> {
                    stmt.setString(1, "s1input");
                    stmt.setString(2, Balance.userAddress(author));
                    // @checkstyle MagicNumber (4 lines)
                    stmt.setBigDecimal(3, this.amount);
                    // @checkstyle LineLengthCheck (1 line)
                    stmt.setString(4, String.format("Receiving BTC operation=%d (by btcops bot)", this.rid));
                    stmt.setString(5, "BTC");
                }
            ).insert(Outcome.VOID);
        // @checkstyle LineLengthCheck (1 line)
        session.sql("UPDATE ioop_requests SET status = 'completed'::ioop_status WHERE rid = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        // @checkstyle LineLengthCheck (1 line)
        session.sql("UPDATE ioop_btc_inputs SET status = 'confirmed'::ioop_btc_state WHERE request = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        session.commit();
    }

    @Override
    public String toString() {
        return String.format(
            "Balance for %d is %s",
            this.rid, this.amount
        );
    }

    /**
     * Encodes user id to transaction dest address.
     * @param uid User id
     * @return String representation of address
     */
    private static String userAddress(final long uid) {
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(uid);
        return String.format(
            "u1%s",
            Base64.getEncoder().encodeToString(
                new UncheckedBytes(
                    new DigestOf(
                        new InputOf(new BytesOf(buffer.array())),
                        new DigestFrom("SHA-256")
                    )
                ).asBytes()
            )
        );
    }
}
