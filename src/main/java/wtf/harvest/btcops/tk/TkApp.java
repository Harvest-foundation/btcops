/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Harvest foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
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

import java.util.regex.Pattern;
import org.bitcoinj.wallet.Wallet;
import org.takes.facets.fork.FkFixed;
import org.takes.facets.fork.FkParams;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.tk.TkWrap;

/**
 * Routing.
 *
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class TkApp extends TkWrap {

    /**
     * Ctor.
     *
     * @param wallet Wallet
     */
    public TkApp(final Wallet wallet) {
        super(
            new TkFork(
                new FkRegex(
                    "/send",
                    new TkSend(wallet)
                ),
                new FkRegex(
                    "/receive",
                    new TkReceive(wallet)
                ),
                new FkRegex(
                    "/balance",
                    new TkFork(
                        new FkParams(
                            "address",
                            Pattern.compile(".+"),
                            new TkBalance(wallet)
                        ),
                        new FkFixed(new TkBalanceTotal(wallet))
                    )
                )
            )
        );
    }
}
