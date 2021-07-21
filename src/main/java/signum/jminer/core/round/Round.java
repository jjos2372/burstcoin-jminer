/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 by luxe - https://github.com/de-luxe - BURST-LUXE-RED2-G6JW-H4HG5
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package signum.jminer.core.round;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import signum.jminer.core.CoreProperties;
import signum.jminer.core.checker.Checker;
import signum.jminer.core.checker.event.CheckerResultEvent;
import signum.jminer.core.network.Network;
import signum.jminer.core.network.event.NetworkQualityChangeEvent;
import signum.jminer.core.network.event.NetworkResultConfirmedEvent;
import signum.jminer.core.network.event.NetworkResultErrorEvent;
import signum.jminer.core.network.event.NetworkStateChangeEvent;
import signum.jminer.core.reader.Reader;
import signum.jminer.core.reader.data.Plots;
import signum.jminer.core.round.event.RoundFinishedEvent;
import signum.jminer.core.round.event.RoundGenSigAlreadyMinedEvent;
import signum.jminer.core.round.event.RoundGenSigUpdatedEvent;
import signum.jminer.core.round.event.RoundSingleResultEvent;
import signum.jminer.core.round.event.RoundSingleResultSkippedEvent;
import signum.jminer.core.round.event.RoundStartedEvent;
import signumj.crypto.SignumCrypto;
import signumj.crypto.plot.impl.MiningPlot;

/**
 * The type Round.
 */
@Component
@Scope("singleton")
public class Round
{
  private static final Logger LOG = LoggerFactory.getLogger(Round.class);

  private final Reader reader;
  private final Checker checker;
  private final Network network;
  private final ApplicationEventPublisher publisher;

  private long targetDeadline;

  private Timer timer;
  private long blockNumber;
  private long finishedBlockNumber;
  private long baseTarget;
  private Date roundStartDate;

  private BigInteger lowestHit;
  private long bestConfirmedDeadline;

  // cache for next lowest
  private int deadlineRetryCounter;
  private BigInteger lowestConfirmed;

  private Set<BigInteger> runningChunkPartStartNonces;
  private Plots plots;
  private byte[] generationSignature;

  // generationSignature
  private Set<String> finishedLookup;

  private long networkSuccessCount;
  private long networkFailCount;

  @Autowired
  public Round(Reader reader, Checker checker, Network network, ApplicationEventPublisher publisher)
  {
    this.reader = reader;
    this.checker = checker;
    this.network = network;
    this.publisher = publisher;

    finishedLookup = new HashSet<>();
  }

  @PostConstruct
  protected void postConstruct()
  {
    timer = new Timer();
  }

  private void initNewRound(Plots plots)
  {
    networkFailCount = 0;
    networkSuccessCount = 0;
    runningChunkPartStartNonces = new HashSet<>(plots.getChunkPartStartNonces().keySet());
    roundStartDate = new Date();
    lowestHit = BigInteger.valueOf(Long.MAX_VALUE);
    lowestConfirmed = BigInteger.valueOf(Long.MAX_VALUE);
    deadlineRetryCounter = 0;
    bestConfirmedDeadline = Long.MAX_VALUE;
  }

  @EventListener
  public void handleMessage(NetworkStateChangeEvent event)
  {
    synchronized(reader)
    {
      boolean blockHeightIncreased = blockNumber < event.getBlockNumber();
      boolean generationSignatureChanged = generationSignature != null && !Arrays.equals(event.getGenerationSignature(), generationSignature);
      boolean restart = false;

      boolean alreadyMined = finishedLookup.contains(SignumCrypto.getInstance().toHexString(event.getGenerationSignature()));
      if(alreadyMined && CoreProperties.isUpdateMiningInfo())
      {
        publisher.publishEvent(new RoundGenSigAlreadyMinedEvent(event.getBlockNumber(), event.getGenerationSignature()));
      }

      if(!blockHeightIncreased && (!alreadyMined && CoreProperties.isUpdateMiningInfo() && generationSignatureChanged))
      {
        restart = true;
        // generationSignature for block updated
        if(finishedBlockNumber == blockNumber)
        {
          finishedBlockNumber--;
        }
        // ui event
        publisher.publishEvent(new RoundGenSigUpdatedEvent(blockNumber, generationSignature));
      }

      long previousBlockNumber = event.getBlockNumber();
      if(blockHeightIncreased)
      {
        previousBlockNumber = blockNumber;
        Round.this.blockNumber = event.getBlockNumber();
      }

      if(blockHeightIncreased || (!alreadyMined && CoreProperties.isUpdateMiningInfo() && generationSignatureChanged))
      {
        Round.this.baseTarget = event.getBaseTarget();
        Round.this.targetDeadline = event.getTargetDeadline();

        long lastBestConfirmedDeadline = bestConfirmedDeadline;

        plots = reader.getPlots();
        int networkQuality = getNetworkQuality();
        initNewRound(plots);

        // reconfigure checker
        generationSignature = event.getGenerationSignature();
        if(CoreProperties.isUseOpenCl())
        {
          checker.reconfigure(blockNumber, generationSignature);
        }

        // Scoop number logic
        int N = event.getNumberOfScoopsPerBlock();
        int scoopArray[] = new int[N];
        SignumCrypto crypto = SignumCrypto.getInstance();
        scoopArray[0] = crypto.calculateScoop(event.getGenerationSignature(), event.getBlockNumber());
        for (int i = 1; i < N; i++) {
          scoopArray[i] = crypto.calculateScoop(crypto.longToBytesBE(scoopArray[i-1]), event.getBlockNumber());
        }
        
        // start reader
        reader.read(previousBlockNumber, blockNumber, generationSignature, scoopArray, lastBestConfirmedDeadline, networkQuality);

        // ui event
        publisher.publishEvent(new RoundStartedEvent(restart, blockNumber, scoopArray, plots.getSize(), targetDeadline, baseTarget, generationSignature));
      }
    }
  }

  @EventListener
  public void handleMessage(CheckerResultEvent event)
  {
    synchronized(reader) {
      if(!isCurrentRound(event.getBlockNumber(), event.getGenerationSignature())) {
        LOG.trace("event for previous block ...");
        return;
      }

      BigInteger nonce = event.getChunkPartStartNonce().add(BigInteger.valueOf(event.getLowestNonce()));
      BigInteger hit = calculateHit(event.getScoops(), generationSignature, event.getLowestNonce());
      event.setResult(hit);
      
      BigInteger deadline = hit.divide(BigInteger.valueOf(baseTarget));
      long calculatedDeadline = deadline.longValue();
      
      if(hit.compareTo(lowestHit) > 0) {
        // ui event
        if(CoreProperties.isShowSkippedDeadlines()) {
          publisher.publishEvent(new RoundSingleResultSkippedEvent(event.getBlockNumber(), nonce, event.getChunkPartStartNonce(), calculatedDeadline,
              targetDeadline));
        }
        // chunkPartStartNonce finished
        runningChunkPartStartNonces.remove(event.getChunkPartStartNonce());
        triggerFinishRoundEvent(event.getBlockNumber());
        
        return;
      }

      lowestHit = hit;
      if(calculatedDeadline < targetDeadline) {
        int[] scoopArray = event.getScoopArray();
        BigInteger numberOfScoops = BigInteger.valueOf(scoopArray.length);
        int scoopNumber = scoopArray[ nonce.add(BigInteger.valueOf(scoopArray[0]))
                                      .multiply(numberOfScoops)
                                      .divide(MiningPlot.SCOOPS_PER_PLOT_BIGINT)
                                      .mod(numberOfScoops).intValue() ];

        network.submitResult(blockNumber, calculatedDeadline, nonce, scoopNumber, event.getChunkPartStartNonce(), plots.getSize(),
            hit, event.getPlotFile());

        // ui event
        publisher.publishEvent(new RoundSingleResultEvent(event, nonce, calculatedDeadline));
      }        
    }
  }

  @EventListener
  public void handleMessage(NetworkResultConfirmedEvent event)
  {
    if(isCurrentRound(event.getBlockNumber(), event.getGenerationSignature()))
    {
      // if result if lower than lowestCommitted, update lowestCommitted
      if(event.getResult() != null && event.getResult().compareTo(lowestConfirmed) < 0) {
        lowestConfirmed = event.getResult();
      }

      runningChunkPartStartNonces.remove(event.getChunkPartStartNonce());

      if(bestConfirmedDeadline > event.getDeadline())
      {
        bestConfirmedDeadline = event.getDeadline();
      }
      triggerFinishRoundEvent(event.getBlockNumber());
    }
  }

  @EventListener
  public void handleMessage(NetworkResultErrorEvent event)
  {
    if(isCurrentRound(event.getBlockNumber(), event.getGenerationSignature())) {
      if(event.getResult()!=null && event.getResult().compareTo(lowestConfirmed) < 0 && event.getStrangeDeadline() >= 0) {
        // we need to send again this one since it was not delivered
        deadlineRetryCounter++;
        if(deadlineRetryCounter < CoreProperties.getDeadlineRetryLimit()) {
          network.submitResult(event.getTask());
          
          return;          
        }
        else {
          LOG.info("retry limit reached");
        }
      }

      runningChunkPartStartNonces.remove(event.getChunkPartStartNonce());
      triggerFinishRoundEvent(event.getBlockNumber());
    }
  }

  @EventListener
  public void handleMessage(NetworkQualityChangeEvent event)
  {
    if(event.isSuccess())
    {
      networkSuccessCount++;
    }
    else
    {
      networkFailCount++;
    }
  }

  private void triggerFinishRoundEvent(long blockNumber)
  {
    if(finishedBlockNumber < blockNumber)
    {
      if(runningChunkPartStartNonces.isEmpty())
      {
        onRoundFinish(blockNumber);
      }
    }
  }

  private void onRoundFinish(long blockNumber)
  {
    finishedBlockNumber = blockNumber;

    // remember finished genSig, to prevent mining it again
    finishedLookup.add(SignumCrypto.getInstance().toHexString(generationSignature));

    long elapsedRoundTime = new Date().getTime() - roundStartDate.getTime();
    int networkQuality = getNetworkQuality();
    timer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        publisher.publishEvent(new RoundFinishedEvent(blockNumber, elapsedRoundTime, networkQuality, plots.getSize()));
      }
    }, 250); // fire deferred

    triggerCleanup();
  }

  private int getNetworkQuality()
  {
    double quality = 1.0 - (double)networkFailCount/(networkSuccessCount + 1.0);
    int percentage = (int)(quality) * 100;
    return percentage;
  }

  private void triggerCleanup()
  {
    TimerTask cleanupTask = new TimerTask()
    {
      @Override
      public void run()
      {
        if(!reader.cleanupReaderPool())
        {
          triggerCleanup();
        }
        else
        {
          System.gc();
        }
      }
    };

    try
    {
      timer.schedule(cleanupTask, 1000);
    }
    catch(IllegalStateException e)
    {
      LOG.error("cleanup task already scheduled ...");
    }
  }

  private boolean isCurrentRound(long currentBlockNumber, byte[] currentGenerationSignature)
  {
    return blockNumber == currentBlockNumber
           && Arrays.equals(generationSignature, currentGenerationSignature);
  }

  private static BigInteger calculateHit(byte[] scoops, byte[] generationSignature, int nonce)
  {
    byte[] scoopData = Arrays.copyOfRange(scoops, nonce * MiningPlot.SCOOP_SIZE, (nonce+1) * MiningPlot.SCOOP_SIZE);
    return SignumCrypto.getInstance().calculateHit(generationSignature, scoopData);
  }
}
