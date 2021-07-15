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

package burstcoin.jminer.core.network.task;

import burstcoin.jminer.core.CoreProperties;
import burstcoin.jminer.core.network.event.NetworkQualityChangeEvent;
import burstcoin.jminer.core.network.event.NetworkStateChangeEvent;
import burstcoin.jminer.core.network.model.MiningInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import signumj.crypto.SignumCrypto;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Network request mining info task.
 */
@Component
@Scope("prototype")
public class NetworkRequestMiningInfoTask
  implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(NetworkRequestMiningInfoTask.class);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher publisher;

  private byte[] generationSignature;
  private long blockNumber;
  private String server;

  private long plotSizeInByte;

  private boolean success;

  @Autowired
  public NetworkRequestMiningInfoTask(HttpClient httpClient, ObjectMapper objectMapper, ApplicationEventPublisher publisher)
  {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.publisher = publisher;
  }

  public void init(String server, long blockNumber, byte[] generationSignature, long plotSizeInByte)
  {
    this.server = server;
    this.generationSignature = generationSignature;
    this.blockNumber = blockNumber;
    this.plotSizeInByte = plotSizeInByte;

    success = false;
  }

  @Override
  public void run()
  {
    LOG.trace("start check network state");

    MiningInfo result = null;
    try
    {
      ContentResponse response = httpClient.newRequest(server + "/burst?requestType=getMiningInfo")
        .timeout(CoreProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      // do not parse result if status code is error
      if(response.getStatus() >= 400)
      {
        LOG.debug("Unable to parse mining info, received http status code " + response.getStatus());
      }
      else
      {
        result = objectMapper.readValue(response.getContentAsString(), MiningInfo.class);
      }

      if(result != null)
      {
        success = true;
        long newBlockNumber = Long.parseUnsignedLong(result.getHeight());
        byte[] newGenerationSignature = SignumCrypto.getInstance().parseHexString(result.getGenerationSignature());

        // higher block 'or' same block with other generationSignature
        if(!Arrays.equals(newGenerationSignature, generationSignature))
        {
          long baseTarget = Long.parseUnsignedLong(result.getBaseTarget());
          long targetDeadline = getTargetDeadline(result.getTargetDeadline(), baseTarget);
          publisher.publishEvent(new NetworkStateChangeEvent(newBlockNumber, baseTarget, newGenerationSignature, targetDeadline));
        }
        else
        {
          LOG.trace("not publish NetworkStateChangeEvent ... '" + newBlockNumber + " <= " + blockNumber + "'");
        }
      }
      else
      {
        LOG.debug("Unable to parse mining info: " + response.getContentAsString());
      }
    }
    catch(TimeoutException timeoutException)
    {
      LOG.debug("Unable to get mining info from wallet, caused by connectionTimeout, currently '"
                + (CoreProperties.getConnectionTimeout() / 1000) + " sec.' try increasing it!");
    }
    catch(Exception e)
    {
      if(e instanceof ConnectException)
      {
        LOG.debug("Unable to get mining info from wallet due ConnectException.");
        LOG.trace("Unable to get mining info from wallet due ConnectException:" + e.getMessage(), e);
      }
      else if(e instanceof EOFException)
      {
        LOG.debug("Unable to get mining info from wallet due EOFException.");
        LOG.trace("Unable to get mining info from wallet due EOFException:" + e.getMessage(), e);
      }
      else
      {
        LOG.debug("Unable to get mining info from wallet.");
        LOG.trace("Unable to get mining info from wallet: " + e.getMessage(), e);
      }
    }
    publisher.publishEvent(new NetworkQualityChangeEvent(success));
  }

  private long getTargetDeadline(long deadlineProvidedByPool, long baseTarget)
  {
    long targetDeadline;
    long defaultTargetDeadline = CoreProperties.getTargetDeadline();
    // ensure default is not 0
    defaultTargetDeadline = defaultTargetDeadline > 0 ? defaultTargetDeadline : Long.MAX_VALUE;

    // 'poolMining' and dynamic deadline
    if(CoreProperties.isPoolMining() && CoreProperties.isDynamicTargetDeadline())
    {
      // please give feedback, in case this is not optimal
      long netDiff = 18325193796L / baseTarget;
      float plotSizeInTiB = plotSizeInByte / 1024 / 1024 / 1024 / 1024;
      targetDeadline = (long) (720 * netDiff / (plotSizeInTiB > 1.0 ? plotSizeInTiB : 1.0));
    }
    // 'soloMining' or 'poolMining and force local deadline'
    else if(!CoreProperties.isPoolMining() || CoreProperties.isForceLocalTargetDeadline())
    {
      targetDeadline = defaultTargetDeadline;
    }
    // 'poolMining'
    else
    {
      targetDeadline = deadlineProvidedByPool > 0 ? deadlineProvidedByPool : defaultTargetDeadline;
    }
    return targetDeadline;
  }
}
