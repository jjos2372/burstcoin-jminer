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
import burstcoin.jminer.core.network.event.NetworkResultConfirmedEvent;
import burstcoin.jminer.core.network.event.NetworkResultErrorEvent;
import burstcoin.jminer.core.network.model.ResponseError;
import burstcoin.jminer.core.network.model.SubmitResult;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.EOFException;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Network submit pool nonce task.
 */
@Component
@Scope("prototype")
public class NetworkSubmitPoolNonceTask
  implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(NetworkSubmitPoolNonceTask.class);
  private static final String HEADER_MINER_NAME = "signum-jminer-0.6.0";

  private final ApplicationEventPublisher publisher;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  private byte[] generationSignature;
  private BigInteger nonce;

  private long blockNumber;
  private BigInteger chunkPartStartNonce;
  private long calculatedDeadline;
  private long totalCapacity;
  private BigInteger result;
  private String plotFilePath;
  private String mac;

  @Autowired
  public NetworkSubmitPoolNonceTask(ApplicationEventPublisher publisher, HttpClient httpClient, ObjectMapper objectMapper)
  {
    this.publisher = publisher;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public void init(long blockNumber, byte[] generationSignature, BigInteger nonce, BigInteger chunkPartStartNonce, long calculatedDeadline, long totalCapacity,
                   BigInteger result, String plotFilePath, String mac)
  {
    this.generationSignature = generationSignature;
    this.nonce = nonce;
    this.blockNumber = blockNumber;
    this.chunkPartStartNonce = chunkPartStartNonce;
    this.calculatedDeadline = calculatedDeadline;
    this.totalCapacity = totalCapacity;
    this.result = result;
    this.plotFilePath = plotFilePath;
    this.mac = mac;
  }

  @Override
  public void run()
  {
    String responseContentAsString = "N/A";
    try
    {
      long gb = totalCapacity / 1000 / 1000 / 1000;

      ContentResponse response = httpClient.POST(CoreProperties.getPoolServer() + "/burst")
        .agent(HEADER_MINER_NAME)
        .param("requestType", "submitNonce")
        .param("accountId", CoreProperties.getNumericAccountId())
        .param("nonce", nonce.toString())
        .param("blockheight", String.valueOf(blockNumber))
        .header("X-Miner", HEADER_MINER_NAME)
        .header("X-Capacity", String.valueOf(gb))

        // thanks @systemofapwne
        .header("X-PlotsHash", StringUtils.isEmpty(mac) ? String.valueOf(gb) : mac) //For CreepMiner: Unique id for system
        .header("X-Deadline", String.valueOf(calculatedDeadline)) //For CreepMiner proxy: Numerical value of this deadline
        .header("X-Plotfile", plotFilePath) //For CreepMiner proxy: Plotfile this deadline origins from

        .timeout(CoreProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      responseContentAsString = response.getContentAsString();

      if(response.getContentAsString().contains("errorCode"))
      {
        ResponseError error = objectMapper.readValue(response.getContentAsString(), ResponseError.class);
        LOG.info("dl '" + calculatedDeadline + "' not accepted by pool!");
        LOG.debug("Error code: '" + error.getErrorCode() + "'.");
        LOG.debug("Error description: '" + error.getErrorDescription() + "'.");
        publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                           chunkPartStartNonce, result));
      }
      else
      {
        SubmitResult result = objectMapper.readValue(response.getContentAsString(), SubmitResult.class);

        if(result.getResult().equals("success"))
        {
          if(calculatedDeadline == result.getDeadline())
          {
            publisher.publishEvent(new NetworkResultConfirmedEvent(blockNumber, generationSignature, result.getDeadline(), nonce, chunkPartStartNonce,
                                                                   this.result));
          }
          else
          {
            // in general if deadlines do not match, we end up in errorCode above
            publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, result.getDeadline(),
                                                               chunkPartStartNonce, this.result));
          }
        }
        else
        {
          LOG.warn("Error: Submit nonce to pool not successful: " + response.getContentAsString());
          publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                             chunkPartStartNonce, this.result));
        }
      }
    }
    catch(TimeoutException timeoutException)
    {
      LOG.warn("Nonce was committed to pool, but not confirmed ... caused by connectionTimeout,"
               + " currently '" + (CoreProperties.getConnectionTimeout() / 1000) + " sec.' try increasing it!");
      publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                         chunkPartStartNonce, this.result));
    }
    catch(ExecutionException e)
    {
      // inform user about reward assignment issue
      if(e.getCause() instanceof HttpResponseException)
      {
        HttpResponseException responseException = (HttpResponseException) e.getCause();
        if(responseException.getResponse() instanceof HttpContentResponse)
        {
          HttpContentResponse httpContentResponse = (HttpContentResponse) responseException.getResponse();
          LOG.warn("Error: Failed to submit nonce to pool: " + httpContentResponse.getContentAsString());
        }
      }
      else
      {
        LOG.warn("Error: Failed to submit nonce to pool due ExecutionException.");
        LOG.debug("ExecutionException: " + e.getMessage(), e);
      }
      publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                         chunkPartStartNonce, this.result));
    }
    catch(EOFException e)
    {
      LOG.warn("Error: Failed to submit nonce to pool due EOFException.");
      LOG.debug("EOFException: " + e.getMessage(), e);
      publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                         chunkPartStartNonce, this.result));
    }
    catch(JsonMappingException e)
    {
      LOG.warn("Error: On submit nonce to pool, could not parse response: '" + responseContentAsString + "'");
      LOG.debug("JSONMappingException: " + e.getMessage(), e);
      publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                         chunkPartStartNonce, this.result));
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to submit nonce to pool due Exception.");
      LOG.debug("Exception: " + e.getMessage(), e);
      publisher.publishEvent(new NetworkResultErrorEvent(blockNumber, generationSignature, nonce, calculatedDeadline, -1L /*not delivered*/,
                                                         chunkPartStartNonce, this.result));
    }
  }
}
