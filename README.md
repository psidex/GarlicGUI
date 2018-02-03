# GarlicGUI

A simple GUI for the Garlicoin pool miners (SGMiner and CCMiner)

# Read before using

**Java needs to be installed to be able to run GarlicGUI**

The default mining intensity is 12

The IP:Port used for the API is `127.0.0.1:4028`

## Requirements

 - [Java](https://java.com/en/download/)

 - [CCMiner](https://github.com/tpruvot/ccminer/releases) (Nvidia) OR [SGminer](https://github.com/nicehash/sgminer-gm/releases) (AMD)

## Instructions

 - Test that the miner works by itself before using this GUI

 - Make sure `Settings/Settings.ser` exists in the same directory as `GarlicGUI.jar`

 - You can use the "Extra miner flags" box to put extra flags such as pool username & password, max-temp for Nvidia GPUs, etc.

The GUI only supplies these options to the miner by default:

`--algorithm scrypt-n --nfactor 11` or `--algo=scrypt:10`

`-o POOL_ADDRESS`

`-u GRLC_ADDRESS`

`-I MINING_INTENSITY` or `-i MINING_INTENSITY`

`--api-listen --api-allow W:127.0.0.1` or `-b 127.0.0.1:4028`

## Warning

I am not responsible for any damage which might happen when mining. Use at your own risk.

# Screenshots

## Setup

![screenshot](screenshot.png)

## Mining

![screenshot2](screenshot2.png)

# Sources

Fira Sans font from https://www.fontsquirrel.com/fonts/fira-sans

---

Donations - `GJbKUzCbAezNZuQJkahqptvT2CpYywMSFj`
