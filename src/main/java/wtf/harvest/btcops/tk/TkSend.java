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
package wtf.harvest.btcops.tk;

import java.io.IOException;
import java.net.HttpURLConnection;
import javax.json.Json;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.takes.HttpException;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.rq.RqHref;
import org.takes.rs.RsJson;

/**
 * Send take.
 *
 * @since 1.0
 */
final class TkSend implements Take {

    /**
     * Wallet.
     */
    private final Wallet wlt;

    /**
     * Ctor.
     *
     * @param wallet Bitcoin wallet
     */
    TkSend(final Wallet wallet) {
        this.wlt = wallet;
    }

    @Override
    public Response act(final Request req) throws IOException {
        final RqHref.Smart href = new RqHref.Smart(req);
        try {
            return new RsJson(
                Json.createObjectBuilder()
                    .add(
                        "tx",
                        this.wlt.sendCoins(
                            SendRequest.to(
                                Address.fromBase58(
                                    this.wlt.getParams(),
                                    href.single("to")
                                ),
                                Coin.parseCoin(href.single("amount"))
                            )
                        ).tx.getHash().toString()
                    ).build()
            );
        } catch (final InsufficientMoneyException err) {
            throw new HttpException(HttpURLConnection.HTTP_BAD_REQUEST, err);
        }
    }
}
