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

package burstcoin.jminer.core.reader.event;

import burstcoin.jminer.core.reader.Reader;
import org.springframework.context.ApplicationEvent;

/**
 * fired on NetworkResultErrorEvent, if a calculated deadline was not matching server result, to deliver affected plot file
 */
@SuppressWarnings("serial")
public class ReaderCorruptFileEvent
  extends ApplicationEvent
{
  private long blockNumber;
  private String filePath;
  private long numberOfChunks;
  private int numberOfParts;

  /**
   * Instantiates a new Reader corrupt file event.
   *
   * @param source the source
   * @param blockNumber the block number
   * @param filePath the file path
   * @param numberOfChunks the number of chunks
   * @param numberOfParts the number of parts
   */
  public ReaderCorruptFileEvent(Reader source, long blockNumber, String filePath, long numberOfChunks, int numberOfParts)
  {
    super(source);
    this.blockNumber = blockNumber;

    this.filePath = filePath;
    this.numberOfChunks = numberOfChunks;
    this.numberOfParts = numberOfParts;
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
   * Gets file path.
   *
   * @return the file path
   */
  public String getFilePath()
  {
    return filePath;
  }

  /**
   * Gets number of chunks.
   *
   * @return the number of chunks
   */
  public long getNumberOfChunks()
  {
    return numberOfChunks;
  }

  /**
   * Gets number of parts.
   *
   * @return the number of parts
   */
  public int getNumberOfParts()
  {
    return numberOfParts;
  }
}
