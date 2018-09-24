package wtf.harvest.btcops;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.ListOutcome;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.cactoos.io.LengthOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.cactoos.io.TeeInput;
import org.cactoos.list.ListOf;
import wtf.harvest.db.PooledDataSource;

/**
 * Bot entry point.
 *
 * @since 1.0
 */
public final class Bot {

    private static final File DATA = new File("/tmp/data");

    private final Params params;
    private final OkHttpClient http;

    Bot(final Params params) {
        this.params = params;
        this.http = new OkHttpClient.Builder()
            .addInterceptor(
                new HttpLoggingInterceptor(
                    message -> Logger.info("HTTP", message)
                ).setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build();
    }

    private Wallet wallet(final NetworkParameters net) throws IOException {
        final File file = new File(Bot.DATA, "btcops.wlt");
        if (!file.exists() || file.isDirectory()) {
            file.delete();
            Wallet.fromWatchingKey(
                net,
                DeterministicKey.deserializeB58(this.params.env("BTC_DPUB"), net)
            ).saveToFile(file);
        }
        try {
            return Wallet.loadFromFile(file);
        } catch (final UnreadableWalletException err) {
            throw new IOException("Failed to read wallet", err);
        }
    }

    private void run() throws Exception {
        final NetworkParameters net = TestNet3Params.get();
        final BlockStore store = new SPVBlockStore(net, Bot.blockchain());
        final BlockChain chain = new BlockChain(net, store);
        final PeerGroup peers = new PeerGroup(net, chain);
        peers.addPeerDiscovery(new DnsDiscovery.DnsSeedDiscovery(net, "testnet-seed.bitcoin.jonasschnelli.ch"));
        final Wallet wlt = this.wallet(net);
        peers.addWallet(wlt);
        chain.addWallet(wlt);
        peers.start();
        peers.downloadBlockChain();
        final DataSource data = new PooledDataSource(this.params.env()).value();
        this.checkInputs(net, data, wlt);
        this.checkBalance(net, data, wlt);
    }

    private void checkInputs(final NetworkParameters net,
        final DataSource data, final Wallet wallet) throws SQLException, IOException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final List<Request> requests =
            session.sql("SELECT ops.id, ops.amount, links.ref AS telegram FROM ioop_requests AS ops JOIN links ON ops.author = links.profile_id WHERE ops.name = 'deposit' AND ops.token = 'BTC' AND ops.status = 'pending' AND links.rel = 'telegram'")
                .select(
                    new ListOutcome<>(
                        rset -> new Request(
                            rset.getLong(1),
                            wallet.freshReceiveAddress(),
                            rset.getBigDecimal(2),
                            rset.getLong(3)
                        )
                    )
                );
        final String telegram = this.params.env("HV_TELEGRAM_HOST");
        for (final Request request : requests) {
            request.assign(session, wallet);
            request.notify(this.http, telegram);
        }
        wallet.saveToFile(new File(Bot.DATA, "btcops.wlt"));
        session.commit();
    }

    private void checkBalance(final NetworkParameters net,
        final DataSource data, final Wallet wallet) throws SQLException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final List<Balance> balances = session.sql("SELECT request, address FROM ioop_btc_inputs WHERE state = 'waiting'")
            .select(
                new ListOutcome<>(
                    rset -> new Balance(
                        rset.getLong(1),
                        new BigDecimal(
                            wallet.getBalance(
                                new AddressBalance(
                                    Address.fromBase58(net, rset.getString(2))
                                )
                            ).toPlainString()
                        )
                    )
                )
            );
        for (final Balance balance : balances) {
            Logger.info(this, "Balance %s", balance);
            balance.accept(data);
        }
    }

    private static File blockchain() {
        final File chain = new File(Bot.DATA, "btcops.chain");
        if (!chain.exists() || chain.isDirectory()) {
            Logger.info(
                Bot.class,
                "extract: deleting existing chain: %b", chain.delete()
            );
            Logger.info(
                Bot.class,
                "extract: extracted SPV blockchain (%d bytes)",
                new LengthOf(
                    new TeeInput(
                        new ResourceOf("spvbstore.blockchain"),
                        new OutputTo(chain)
                    )
                ).longValue()
            );
        }
        Logger.info(
            Bot.class,
            "extract: chain file: %s", chain.getAbsolutePath()
        );
        return chain;
    }

    public static void main(String[] args) throws Exception {
        new Bot(new Params(new ListOf<>(args))).run();
    }

}
