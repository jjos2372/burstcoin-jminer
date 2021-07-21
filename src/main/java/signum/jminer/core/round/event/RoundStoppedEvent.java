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

/**
 * The type Round stopped event.
 */
@SuppressWarnings("serial")
public class RoundStoppedEvent
  extends ApplicationEvent
{
  private long blockNumber;

  private long capacity;
  private long remainingCapacity;
  private long elapsedTime;
  private int networkQuality;

  public RoundStoppedEvent(long blockNumber, long capacity, long remainingCapacity, long elapsedTime, int networkQuality)
  {
    super(blockNumber);

    this.blockNumber = blockNumber;
    this.capacity = capacity;
    this.remainingCapacity = remainingCapacity;
    this.elapsedTime = elapsedTime;
    this.networkQuality = networkQuality;
  }

  public long getBlockNumber()
  {
    return blockNumber;
  }

  public long getCapacity()
  {
    return capacity;
  }

  public long getRemainingCapacity()
  {
    return remainingCapacity;
  }

  public long getElapsedTime()
  {
    return elapsedTime;
  }

  public int getNetworkQuality()
  {
    return networkQuality;
  }
}

