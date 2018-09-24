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
 *
 * @since
 */
final class Balance {
    private final long rid;
    private final BigDecimal val;

    Balance(final long rid, final BigDecimal val) {
        this.rid = rid;
        this.val = val;
    }

    public void accept(final DataSource data) throws SQLException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final Long author = session.sql("SELECT author FROM ioop_requests WHERE id = ?")
            .set(this.rid)
            .select(new SingleOutcome<>(Long.class));
        session.sql("INSERT INTO ledger (src, dst, amount, details, token) VALUES (?, ?, ?,?, ?)")
            .prepare(
                stmt -> {
                    stmt.setString(1, "s1input");
                    stmt.setString(2, Balance.userAddress(author));
                    stmt.setBigDecimal(3, this.val);
                    stmt.setString(4, String.format("Receiving BTC operation=%d (by btcops bot)", this.rid));
                    stmt.setString(5, "BTC");
                }
            ).insert(Outcome.VOID);
        session.sql("UPDATE ioop_requests SET status = 'completed'::ioop_status WHERE id = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        session.sql("UPDATE ioop_btc_inputs SET status = 'confirmed'::ioop_btc_state WHERE request = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        session.commit();
    }

    @Override
    public String toString() {
        return String.format(
            "Balance for %d is %s",
            this.rid, this.val
        );
    }

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
