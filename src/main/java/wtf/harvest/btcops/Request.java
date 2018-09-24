package wtf.harvest.btcops;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.json.Json;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bitcoinj.core.Address;
import org.bitcoinj.wallet.Wallet;

/**
 *
 * @since
 */
final class Request {
    private final long rid;
    private final Address addr;
    private final BigDecimal amount;
    private final long telegram;
    Request(final long rid, final Address addr, final BigDecimal amount, final long telegram) {
        this.rid = rid;
        this.addr = addr;
        this.amount = amount;
        this.telegram = telegram;
    }

    public void assign(final JdbcSession session, final Wallet wallet) throws SQLException {
        session.sql("UPDATE ioop_requests SET status = 'assigned'::ioop_status WHERE id = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        session.sql("INSERT INTO ioop_btc_inputs (request, address) VALUES (?, ?)")
            .set(this.rid)
            .set(this.addr)
            .insert(Outcome.VOID);
        wallet.addWatchedAddress(this.addr);
        Logger.info(
            this,
            "Assigned request (%d) to btcops bot; generated BTC address (%s) to receive %s coins",
            this.rid, this.addr.toBase58(), this.amount.toString()
        );
    }

    public void notify(final OkHttpClient http, final String host) throws IOException {
        if (host == null) {
            Logger.info(
                this,
                "Notification for request %d has been sent to telegram user (%d): expecting %s coins on %s address",
                this.rid, this.telegram, this.amount.toString(), this.addr.toBase58()
            );
            return;
        }
        final Response resp = http.newCall(
            new okhttp3.Request.Builder()
                .url(
                    new HttpUrl.Builder()
                        .scheme("http")
                        .host(host)
                        .encodedPath(String.format("/notifications/%d", this.telegram))
                        .build()
                )
                .post(
                    RequestBody.create(
                        MediaType.parse("application/json"),
                        Json.createObjectBuilder()
                            .add(
                                "text",
                                String.format(
                                    "Send %s BTC to address `%s`",
                                    this.amount.toString(), this.addr.toBase58()
                                )
                            )
                            .build()
                            .toString()
                    )
                )
                .build()
        ).execute();
        if (!resp.isSuccessful()) {
            throw new IOException(
                String.format("Failed to notify the user: %d", resp.code())
            );
        }
    }
}
