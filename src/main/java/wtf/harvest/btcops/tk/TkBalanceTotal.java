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
import java.math.BigDecimal;
import javax.json.Json;
import org.bitcoinj.wallet.Wallet;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.rs.RsJson;

/**
 * Total balance take.
 *
 * @since 1.0
 */
final class TkBalanceTotal implements Take {

    /**
     * Wallet.
     */
    private final Wallet wlt;

    /**
     * Ctor.
     *
     * @param wallet Bitcoin wallet
     */
    TkBalanceTotal(final Wallet wallet) {
        this.wlt = wallet;
    }

    @Override
    public Response act(final Request req) throws IOException {
        return new RsJson(
            Json.createObjectBuilder()
                .add(
                    "balance",
                    new BigDecimal(this.wlt.getBalance().toPlainString())
                        .toString()
                ).build()
        );
    }
}
