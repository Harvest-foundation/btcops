# btcops

Bitcoin wallet operations service.
It provides simple API for most common wallet operations: (send, receive and check balance).

[![EO principles respected here](http://www.elegantobjects.org/badge.svg)](http://www.elegantobjects.org)
[![Managed by Zerocracy](https://www.0crat.com/badge/CAWJH9K0X.svg)](https://www.0crat.com/p/CAWJH9K0X)
[![DevOps By Rultor.com](http://www.rultor.com/b/Harvest-foundation/btcops)](http://www.rultor.com/p/Harvest-foundation/btcops)

[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/Harvest-foundation/btcops/blob/master/LICENSE.txt)
[![PDD status](http://www.0pdd.com/svg?name=Harvest-foundation/btcops)](http://www.0pdd.com/p?name=Harvest-foundation/btcops)

## API

### Send BTC

API sens BTC to address.

**Method:** `POST`

**URL:** `/send`

**Query params:**
 - `to` - destination address
 - `amount` - amount of BTC to send

**Response:** `application/json` with fields:
 - `tx` - transaction hash: JSON string

**Errors:**
 - `400` (bad request) - if wallet doesn't have enough coins

*Example:*

`http POST http://localhost:8888/send?to=mfwcs8AZ7dtbiA27AHQgh3Ne252iWRbYB9&amount=0.001`

### Receive BTC

API to create new address for receiving.

**Method:** `POST`

**URL:** `/receive`

**Response:** `application/json` with fields:
 - `address` - fresh address for receiving: JSON string

*Example:*

`http POST http://localhost:8888/receive`
```json
{
  "address": "mfwcs8AZ7dtbiA27AHQgh3Ne252iWRbYB9"
}
```

### Check balance

API to check balance on address or total wallet balance.

**Method:** `GET`

**URL:** `/balance`

**Query params:**
 - `address` (optional) - ask API for balance on concrete address

 **Response:** `application/json` with fields:
  - `balance` - amount of BTC: JSON string

 *Example:*
 `http GET http://localhost:8888/balance`
 ```json
 {
   "balance": "0.002"
 }
 ```

 ## Usage
 It's designed to be used as a docker image (see `Dockerfile`),
 some configuration options:
  - `--port` - service port
  - `--discovery` - peer discovery seed (can be multiple options)
  - `--net` - `test3` for test3net or `main` for mainnet
  - `--data` - data directory where blockchain and wallet file will be stored

*Example configuration:*
```Dockerfile
 CMD [ \
     "-Dfile.encoding=UTF-8",  \
     "-cp", "service.jar:deps/*", \
     "wtf.harvest.btcops.BtcOps", \
     "--port=80", \
     "--discovery=testnet-seed.bitcoin.petertodd.org", \
     "--discovery=testnet-seed.bluematt.me", \
     "--data=/var/btcops", \
     "--net=test3" \
 ] ```
