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

package burstcoin.jminer.core.network.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigInteger;

/**
 * fired if server response deadline does not match calculated deadline.
 */
@SuppressWarnings("serial")
public class NetworkResultErrorEvent
  extends ApplicationEvent
{
  private long blockNumber;
  private byte[] generationSignature;
  private BigInteger nonce;

  private long calculatedDeadline;
  private long strangeDeadline;

  private BigInteger chunkPartStartNonce;
  private BigInteger result;

  /**
   * Instantiates a new Network result error event.
   *
   * @param blockNumber the block number
   * @param nonce the nonce
   * @param calculatedDeadline the calculated deadline
   * @param strangeDeadline the strange deadline
   * @param chunkPartStartNonce the chunk part start nonce
   */
  public NetworkResultErrorEvent(long blockNumber, byte[] generationSignature, BigInteger nonce, long calculatedDeadline, long strangeDeadline, BigInteger chunkPartStartNonce, BigInteger result)
  {
    super(blockNumber);

    this.blockNumber = blockNumber;
    this.generationSignature = generationSignature;
    this.nonce = nonce;
    this.calculatedDeadline = calculatedDeadline;
    this.strangeDeadline = strangeDeadline;

    this.chunkPartStartNonce = chunkPartStartNonce;
    this.result = result;
  }

  /**
   * Gets chunk part start nonce.
   *
   * @return the chunk part start nonce
   */
  public BigInteger getChunkPartStartNonce()
  {
    return chunkPartStartNonce;
  }

  /**
   * Gets block number.
   *
   * @return the block number
   */
  public long getBlockNumber()
  {
    return blockNumber;
  }

  /**
   * Gets nonce.
   *
   * @return the nonce
   */
  public BigInteger getNonce()
  {
    return nonce;
  }

  /**
   * Gets calculated deadline.
   *
   * @return the calculated deadline
   */
  public long getCalculatedDeadline()
  {
    return calculatedDeadline;
  }

  /**
   * Gets strange deadline.
   *
   * @return the strange deadline
   */
  public long getStrangeDeadline()
  {
    return strangeDeadline;
  }

  public BigInteger getResult()
  {
    return result;
  }

  public byte[] getGenerationSignature()
  {
    return generationSignature;
  }
}
