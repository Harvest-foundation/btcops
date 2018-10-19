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
package wtf.harvest.btcops;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.takes.http.Exit;
import org.takes.http.FtCli;
import wtf.harvest.btcops.tk.TkApp;

/**
 * Bot entry point.
 *
 * @since 1.0
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
public final class BtcOps {

    /**
     * Data folder.
     */
    private final File data;

    /**
     * Wallet file.
     */
    private final File wfile;

    /**
     * Bot parameters.
     */
    private final Params params;

    /**
     * Ctor.
     * @param params Bot parameters
     * @param data Data directory
     * @param wfile Wallet file
     */
    BtcOps(final Params params, final File data, final File wfile) {
        this.params = params;
        this.data = data;
        this.wfile = wfile;
    }

    /**
     * Primary ctor.
     *
     * @param params Bot parameters.
     */
    BtcOps(final Params params) {
        this(
            params,
            params.arg().data(),
            new File(params.arg().data(), "btcops.wlt")
        );
    }

    /**
     * Bot entrypoint.
     * @param args Bot parameters
     * @throws Exception If an error occurs
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static void main(final String... args) throws Exception {
        new BtcOps(new Params(args)).run();
    }

    /**
     * Wallet from the network.
     * @param net Network
     * @return Wallet
     * @throws IOException If something goes wrong
     */
    private Wallet walletFrom(final NetworkParameters net) throws IOException {
        if (!this.wfile.exists() || this.wfile.isDirectory()) {
            this.wfile.delete();
            new Wallet(net).saveToFile(this.wfile);
        }
        try {
            return Wallet.loadFromFile(this.wfile);
        } catch (final UnreadableWalletException err) {
            throw new IOException("Failed to read wfile", err);
        }
    }

    /**
     * Run this bot.
     * @throws Exception If something goes wrong
     */
    private void run() throws Exception {
        final NetworkParameters net = this.params.arg().net();
        Logger.info(this, "Using network: %s", net);
        final BlockStore store = new SPVBlockStore(
            net,
            new File(this.data, "btcops.chain")
        );
        final BlockChain chain = new BlockChain(net, store);
        final PeerGroup peers = new PeerGroup(net, chain);
        for (final String seed : this.params.arg().discovery()) {
            Logger.info(
                this,
                "Using peer discovery seed: %s", seed
            );
            peers.addPeerDiscovery(
                new DnsDiscovery.DnsSeedDiscovery(net, seed)
            );
        }
        final Wallet wlt = this.walletFrom(net);
        peers.addWallet(wlt);
        chain.addWallet(wlt);
        peers.start();
        peers.downloadBlockChain();
        new FtCli(
            new TkApp(wlt),
            String.format("--port=%d", this.params.arg().port()),
            "--threads=4"
        ).start(Exit.NEVER);
    }
}
