# signum-jminer

GPU/CPU optimized Signum miner.

1. edit `jminer.properties` with text editor to configure
2. ensure java v8+ (64bit) and openCL driver/sdk is installed (java9 will not work)
3. execute the EXE file if on Windows and the SH file if on Linux/Mac

> '-d64' to ensure 64bit java (remove for 32bit)
> '-XX:+UseG1GC' to free memory after round finished.


The miner is configured in a text-file named 'jminer.properties'.
This file has to be in the miner directory (same folder as '*.jar' file) 
> To get started, use one of the following examples. 
> The min. required settings for the different mining-modes.

# Pool-Setup

    plotPaths=D:/,C:/,E:/plots,F:/plots
    numericAccountId=<YOUR NUMERIC ACCOUNT ID>
    poolServer=http://pool.com:port

# Solo-Setup

    plotPaths=D:/,C:/,E:/plots,F:/plots
    poolMining=false
    passPhrase=<YOUR PASS PHRASE>

# POC1 and POC2 - NOTICE
jminer still supports POC1

only use one type, POC1 or POC2 on one drive ('plotPath'), mixed will be skipped for now.

ensure your POC2 plotfiles do not have staggersize in filename, or they will be treated like POC1.

      POC1 filename: 'numericAccountId_startNonce_numberOfNonces_staggersize'
      POC2 filename: 'numericAccountId_startNonce_numberOfNonces'


# List of all properties
your 'jminer.properties' hasn't got to contain all properties listed here,
most of them are optional or there is a fallback/default value for it.


## Plot-Files

### plotPaths (required)
list of plot paths separated with , e.g. D:/,C:/,E:/plots,F:/plots (in one line)
the miner will treat every path as 'physical' drive and use one thread for it

    plotPaths=D:/,C:/,E:/plots,F:/plots

### scanPathsEveryRound (default:true)  
optional 'true' will check 'plotPaths' for changed plot files on every round 'false' will check only on start/restart
if you are moving/creating plot-files while mining, it could be disabled

    scanPathsEveryRound=false

### listPlotFiles (default:false)
optional ... list all plotFiles on start. If walletServer/soloServer is configured, 
it will show mined blocks and drive seeks/chunks of plotfile, too.

    listPlotFiles=true



## Mining Mode and Target Deadline

### poolMining (default:true)
'true' for pool mining, 'false' for solo mining. ensure to configure the chosen mining-mode below.
For solo-mining you need to set

    poolMining=false

### targetDeadline (optinal)
min. deadline to be committed. Will be used for pool mining if 'forceLocalTargetDeadline=true'

    targetDeadline=750000

### forceLocalTargetDeadline (default:false)
'true' will force jminer to use the targetDeadline specified below, 
even if the pool says otherwise.  Only for pool mining! 'false' uses the default behavior for targetDeadline.
https://www.youtube.com/watch?v=9lwogE31SiI

    forceLocalTargetDeadline=true

### dynamicTargetDeadline (default:false)
'true' will overrule 'targetDeadline' and 'forceLocalTargetDeadline' settings.
the miner will calculate the targetDeadline dynamic on poolMining.

    dynamicTargetDeadline=true
    



## Pool-Mining
Ensure you already setup reward assignment http://localhost:8125/rewardassignment.html

### numericAccountId (required for pool)
first number in all plot-files

    numericAccountId=xxxxxxxxxxxxxxx


### poolServer (required for pool)
format is inclusive protocol and port e.g. 'http://pool.com:8125'

    poolServer=http://pool.com


## Solo-mining

### soloServer (default:http://localhost:8125)
**WARN!** soloServer should be http://localhost:8125 or http://127.0.0.1:8125
Solo means you send your PASS when submitting results!

**DO NOT** try to use a online wallet or pool as Server!

    soloServer=http://127.0.0.1:8125

### passPhrase (required for solo)
secretPhrase/password of solo mining burst-account

    passPhrase=xxxxxxxxxxxxxx



## OpenCL
The miner can use openCL for most of the mining calculations, ensure it is setup correctly.
Instructions can be found e.g. here (thanks cryo):
https://github.com/bhamon/gpuPlotGenerator/blob/master/README.md
You could also use that instruction to find your platformId and deviceId if needed.
Since version 0.4.4 all available platforms and devices are listed on startup.

### useOpenCl (default:false)
To enable openCL

    useOpenCl=true

### platformId (default:0) 
id of openCL platform on your system. one platform may have multiple
devices, the miner currently uses just one (in general not the bottleneck)

    platformId=0

### deviceId (default:0)
specifies the device used by OCLCecker, can be your first GPU,
in most cases it will not be 100% used. (depends on capacity)

    deviceId=1



## Miner Internals

### refreshInterval (default:2000)
interval of asking wallet/pool for mining info (in ms), to check for new block

    refreshInterval=2000

### updateMiningInfo (default=true)
restart round on new generationSignature for same round 
as long as equal miningInfo was not already finished
(it may happen that miningInfo changes, and changes back later)
false value will disable this feature

    updateMiningInfo=false

### connectionTimeout (default:12000)
increase the 'connectionTimeout' on network problems. this timeout is used for all network requests.
if you use pool or online-wallet, the 12 sec. default may cause timeout on submitting nonces 
or getting mining info etc.

    connectionTimeout=12000

### debug (default:false)
setting 'debug' to true will log additional information of the mining process,
that are not related to mining, but to miner internals.

    debug=true

### writeLogFile (default:false)
setting 'writeLogFile' to 'true' will write all logs from console to a file, too.
the name of that file can be specified by 'logFilePath'.

    writeLogFile=true

### logFilePath (default:log/jminer.log.txt)
path (filename and optional directory, relative to miner location)

    logFilePath=mylogs/jminier/log.txt


## Miner Appearance

### readProgressPerRound (default:9) 
defines how often the mining progress is shown per round
thats the 'xx% done ...' info.

    readProgressPerRound=16

### byteUnitDecimal (default:true) 
switch between decimal units (true): TB/GB/MB (divided by 1000),
or binary units (false) TiB/GiB/MiB (divided by 1024) - https://en.wikipedia.org/wiki/Byte

    byteUnitDecimal=false

### showDriveInfo (default:false)
set this to 'true' to show info about every drive on finish reading it,
this is useful to find the slow ones ... can help to optimize your setup.

    showDriveInfo=true

e.g. you see in logs, that 'drive-c' and 'drive-d' slow down this mining setup:

    read 'C:/data/drive-a' (3TB 958GB) in '28s 444ms'
    read 'C:/data/drive-b' (3TB 932GB) in '29s 114ms'
    read 'C:/data/drive-c' (3TB 996GB) in '35s 390ms'
    read 'C:/data/drive-d' (3TB 996GB) in '35s 685ms'

### logPatternConsole & logPatternFile
patterns for logfile and cosole output can be different
only needed if you want to configure your own log pattern e.g.
following would show colored time and message on console:

    logPatternConsole=%blue(%d{HH:mm:ss.SSS}) %green(%msg%n)
    
only time and message for logfile:
    
    logPatternFile=%d{HH:mm:ss.SSS} %msg%n
    
For all options please read from docs: 
https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout

### showSkippedDeadlines (default:true) 
set this to 'true' to show found deadlines below targetDeadline configure or 
provided by pool (overwriting the targetDeadline specified in jminer.properties)

    showSkippedDeadlines=false


## Miner Memory Usage
 
### chunkPartNonces (default:960000)
staggerSize defines number of nonces per chunk.
the miner will split chunks in smaller pieces called chunkParts.
this makes sense, to save memory and optimize speed.
in the best case chunkPart#1 will be checked before chunkPart#2 is
completely read ... depending on the power of your GPU.
if staggersize is smaller than chunkPartNonces, staggersize will be used.
e.g. play with +/- 160000 steps

    chunkPartNonces=960000 

### readerThreads (default:0)
normally '0' means, the miner takes one thread per drive (plotPath) this is recommend.
choosing another number of 'readerThreads' can be useful on memory issues.
For example, if you mine on 4 drives (plotPaths), you can reduce the memory usage
by setting 'readerThreads=2', this will reduce mining speed but save memory.

    readerThreads=10
    
    



