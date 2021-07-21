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

package signum.jminer.core.round.event;

import org.springframework.context.ApplicationEvent;

import signum.jminer.core.checker.event.CheckerResultEvent;

import java.math.BigInteger;

/**
 * fired on new best deadline below targetDeadline (not confirmed)
 */
@SuppressWarnings("serial")
public class RoundSingleResultEvent
  extends ApplicationEvent
{
  private long blockNumber;
  private BigInteger nonce;

  private long calculatedDeadline;
  private CheckerResultEvent event;

  /**
   * Instantiates a new Round single result event.
   *
   * @param blockNumber the block number
   * @param nonce the nonce
   * @param chunkPartStartNonce the chunk part start nonce
   * @param calculatedDeadline the calculated deadline
   * @param poolMining the pool mining
   */
  public RoundSingleResultEvent(CheckerResultEvent event, BigInteger nonce, long calculatedDeadline)
  {
    super(event.getBlockNumber());

    this.event = event;
    this.nonce = nonce;
    this.calculatedDeadline = calculatedDeadline;
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
   * @return the original event
   */
  public CheckerResultEvent getCheckerResultEvent()
  {
    return event;
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

}
