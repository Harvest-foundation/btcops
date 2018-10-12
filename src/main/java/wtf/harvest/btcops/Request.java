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
 * Request for btc operation.
 *
 * @since 2.0
 */
final class Request {
    /**
     * Request id.
     */
    private final long rid;

    /**
     * Receiver address.
     */
    private final Address addr;

    /**
     * Request amount.
     */
    private final BigDecimal amount;

    /**
     * User (broker) telegram id.
     */
    private final long telegram;

    /**
     * Ctor.
     * @param rid Request id.
     * @param addr Address
     * @param amount Amount
     * @param telegram Telegram id
     * @checkstyle ParameterNumberCheck (4 lines)
     */
    Request(final long rid, final Address addr, final BigDecimal amount,
        final long telegram) {
        this.rid = rid;
        this.addr = addr;
        this.amount = amount;
        this.telegram = telegram;
    }

    /**
     * Assigns request to btcops bot.
     * @param session Db session
     * @param wallet Wallet
     * @throws SQLException If smth goes wrong
     */
    public void assign(final JdbcSession session, final Wallet wallet)
        throws SQLException {
        session
            // @checkstyle LineLengthCheck (1 line)
            .sql("UPDATE ioop_requests SET status = 'assigned'::ioop_status WHERE id = ?")
            .set(this.rid)
            .update(Outcome.VOID);
        session
            .sql("INSERT INTO ioop_btc_inputs (request, address) VALUES (?, ?)")
            .set(this.rid)
            .set(this.addr)
            .insert(Outcome.VOID);
        wallet.addWatchedAddress(this.addr);
        Logger.info(
            this,
            // @checkstyle LineLengthCheck (1 line)
            "Assigned request (%d) to btcops bot; generated BTC address (%s) to receive %s coins",
            this.rid, this.addr.toBase58(), this.amount.toString()
        );
    }

    /**
     * Notifies telegram user about operation.
     * @param http Http client
     * @param host Telegram app host
     * @throws IOException If smth went wrong
     */
    public void notify(final OkHttpClient http, final String host)
        throws IOException {
        if (host == null) {
            Logger.info(
                this,
                // @checkstyle LineLengthCheck (1 line)
                "Notification for request %d has been sent to telegram user (%d): expecting %s coins on %s address",
                this.rid, this.telegram,
                this.amount.toString(), this.addr.toBase58()
            );
            return;
        }
        final Response resp = http.newCall(
            new okhttp3.Request.Builder()
                .url(
                    new HttpUrl.Builder()
                        .scheme("http")
                        .host(host)
                        .encodedPath(
                            String.format("/notifications/%d", this.telegram)
                        )
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
